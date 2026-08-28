package ai.scheduler;

import java.io.File;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.inbound.FileReadingMessageSource;
import org.springframework.integration.file.filters.AbstractFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.LastModifiedFileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.RecursiveDirectoryScanner;

import ai.AppProperties;
import ai.dto.own.response.DataIngestionResponseDto;
import ai.enums.DataSource;
import ai.enums.DataScope;
import ai.enums.IngestionStatus;
import ai.service.DataIngestionService;
import ai.service.OrganizationService;
import ai.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DataIngestionAutoImportScheduler {
	DataIngestionService dataIngestionService;
	AppProperties appProperties;
    UserService userService;
    OrganizationService organizationService;

    // Định nghĩa luồng xử lý tự động file để ingest dữ liệu: lấy file từ thư mục đầu vào, di chuyển file sang thư mục xử lý tạm thời, gọi service để ingest file, sau đó di chuyển file đã xử lý xong sang thư mục failed nếu có lỗi hoặc xóa file nếu ingest thành công và cấu hình cho phép xóa
	@Bean
	IntegrationFlow autoIngestionFlow() {
		return IntegrationFlow
				.from(autoIngestionFileSource(), c -> c.poller(Pollers.fixedDelay(resolvePollerDelayMs()) // Cấu hình thời gian giữa 2 lần quét thư mục để tìm file mới, đơn vị là ms. Mặc định 60000ms (60 giây)
				.maxMessagesPerPoll(resolveMaxMessagesPerPoll()))) // Giới hạn số file được xử lý trong mỗi lần quét thư mục để tránh quá tải. Mặc định 50 file/lần quét
				.handle(File.class, (file, headers) -> {
					processFile(file);
					return null;
				})
				.get();
	}

    // Định nghĩa nguồn file để luồng xử lý tự động quét: sử dụng FileReadingMessageSource của Spring Integration để quét thư mục đầu vào, với bộ lọc tùy chỉnh để chỉ lấy file (không lấy thư mục), nằm trong thư mục đầu vào (không lấy file trong thư mục xử lý tạm thời hoặc thư mục failed), đã ổn định (không còn đang được ghi dữ liệu) và chưa từng được xử lý trước đó
	@Bean
	FileReadingMessageSource autoIngestionFileSource() {
		log.info("Initializing auto-ingestion file source...");
		ensureDirectoryExists(resolveInputDir());
		ensureDirectoryExists(resolveProcessingDir());
		ensureDirectoryExists(resolveFailedDir());
		// Khôi phục các file còn sót trong thư mục processing (do app crash/restart giữa chừng) về input để xử lý lại
		recoverStaleProcessingFiles();

		RecursiveDirectoryScanner scanner = new RecursiveDirectoryScanner();
		scanner.setFilter(buildFilter());

		FileReadingMessageSource source = new FileReadingMessageSource();
		source.setDirectory(resolveInputDir().toFile());
		source.setScanner(scanner);
		// Recursive scanner and watch service cannot be enabled together in SI 7.x.
		source.setUseWatchService(false);
		source.setAutoCreateDirectory(true);
		return source;
	}

    // Xây dựng filter để chỉ lấy file (không lấy thư mục), nằm trong thư mục đầu vào (không lấy file trong thư mục xử lý tạm thời hoặc thư mục failed), đã ổn định (không còn đang được ghi dữ liệu) và chưa từng được xử lý trước đó
	private CompositeFileListFilter<File> buildFilter() {
		log.info("Building file filter for auto-ingestion...");
		CompositeFileListFilter<File> filter = new CompositeFileListFilter<File>();
        // Chỉ lấy file khi auto-ingestion được bật. Filter này phải đặt TRƯỚC AcceptOnceFileListFilter để khi tính năng bị tắt,
        // file không bị đánh dấu là "đã xử lý" — nếu không, các file đang có sẵn trong thư mục đầu vào sẽ bị bỏ qua vĩnh viễn khi bật lại tính năng.
		filter.addFilter(new AbstractFileListFilter<File>() {
			@Override
			public boolean accept(File file) {
				return isEnabled();
			}
		});
        // Lấy tất cả file, sau đó sẽ lọc tiếp ở filter bên dưới để đảm bảo chỉ lấy file nằm trong thư mục đầu vào, không lấy file trong thư mục xử lý tạm thời hoặc thư mục failed
		filter.addFilter(new RegexPatternFileListFilter(".*"));
        // Lọc file để chỉ lấy file (không lấy thư mục), nằm trong thư mục đầu vào (không lấy file trong thư mục xử lý tạm thời hoặc thư mục failed)
		filter.addFilter(new AbstractFileListFilter<File>() {
			@Override
			public boolean accept(File file) {
				if (file == null || !file.isFile()) {
					return false;
				}

				Path inputPath = resolveInputDir();
				Path processingPath = resolveProcessingDir();
				Path failedPath = resolveFailedDir();

				Path normalized = file.toPath().toAbsolutePath().normalize();
				if (!normalized.startsWith(inputPath)) {
					return false;
				}
				if (normalized.startsWith(processingPath) || normalized.startsWith(failedPath)) {
					return false;
				}
				return true;
			}
		});
        // Lọc file để chỉ lấy file đã ổn định (không còn đang được ghi dữ liệu) và chưa từng được xử lý trước đó, tránh tình trạng file bị xử lý khi đang được ghi dữ liệu hoặc bị xử lý đồng thời
		filter.addFilter(new LastModifiedFileListFilter(resolveStableFileAgeMs() / 1000));
        // Ghi chú: không dùng AcceptOnceFileListFilter vì việc move file sang thư mục processing (atomic move) đã đảm bảo chống xử lý trùng.
        // Hơn nữa, AcceptOnce là filter in-memory sẽ chặn vĩnh viễn các file trùng tên và khiến file không thể retry sau lỗi tạm thời.
		return filter;
	}

    // Xử lý file: di chuyển file từ thư mục đầu vào sang thư mục xử lý tạm thời, gọi service để ingest file, sau đó di chuyển file đã xử lý xong sang thư mục failed nếu có lỗi hoặc xóa file nếu ingest thành công và cấu hình cho phép xóa
	private void processFile(File file) {
		log.info("Processing file for auto-ingestion: {}", file.getAbsolutePath());
		if (!isEnabled()) {
			return;
		}

		if (!file.exists()) {
			log.warn("Auto-ingestion skipped: file was deleted before processing could begin. file={}", file.getAbsolutePath());
			return;
		}

		Path inputDir = resolveInputDir();
		Path originalFile = file.toPath().toAbsolutePath().normalize();
		if (!originalFile.startsWith(inputDir)) {
			return;
		}

		Path relativePath = inputDir.relativize(originalFile);
		if (relativePath.getNameCount() == 0) {
			return;
		}

		Path stagedFile = moveToProcessing(originalFile, relativePath);
		if (stagedFile == null) {
			return;
		}

		try {
			UUID ownerId = userService.getRoot().getId();
			UUID orgId = organizationService.getRoot().getId();

			if (ownerId == null || orgId == null) {
				log.error("Auto-ingestion skipped because owner-id or organization-id is missing/invalid. file={}", originalFile);
				moveToFailed(stagedFile, relativePath);
				return;
			}

			DataScope accessLevel = appProperties.getAutoIngestion().getAccessLevel() == null
					? DataScope.GLOBAL
					: appProperties.getAutoIngestion().getAccessLevel();

			DataSource fromSource = appProperties.getAutoIngestion().getFromSource() == null
					? DataSource.SYSTEM
					: appProperties.getAutoIngestion().getFromSource();

			DataIngestionResponseDto response = dataIngestionService.ingestLocalFile(
					stagedFile,
					relativePath,
					ownerId,
					orgId,
					accessLevel,
					fromSource);

			// Nếu ingest thất bại (đẩy sang RAG lỗi), thì dựa vào retryCount để quyết định:
			// - Nếu chưa vượt quá maxRetryAttempts: đưa file trở lại thư mục input để retry ở lần quét sau.
			//   Record FAILED cũ sẽ được tái sử dụng (không tạo mới) nên không gây trùng lắp dữ liệu.
			// - Nếu đã vượt quá maxRetryAttempts: move file sang thư mục failed để dừng hẳn, tránh retry vô hạn.
			if (response.getIngestionStatus().equals(IngestionStatus.FAILED.name())) {
				int retryCount = response.getRetryCount() == null ? 0 : response.getRetryCount();
				if (retryCount < resolveMaxRetryAttempts()) {
					log.warn("Auto-ingestion push to ingestion service failed, moving file back to input for retry. attempt={}/{}. file={}",
							retryCount, resolveMaxRetryAttempts(), originalFile);
					moveBackToInput(stagedFile, relativePath);
				} else {
					log.error("Auto-ingestion failed after exceeding max retry attempts ({}). Moving file to failed area. file={}",
							resolveMaxRetryAttempts(), originalFile);
					moveToFailed(stagedFile, relativePath);
				}
				return;
			}

			// Nếu ingest thành công và cấu hình cho phép xóa file, thì xóa file đã ingest khỏi thư mục xử lý tạm thời
			if (appProperties.getAutoIngestion().isDeleteLocalAfterSuccess()) {
				Files.deleteIfExists(stagedFile);
			}
		} catch (Exception exception) {
			// Ngoại lệ (vd ingestion service tạm downtime, DB lỗi, thiếu root user...) thường là lỗi tạm thời.
			// Không move sang .failed ngay mà đưa file trở lại thư mục input để retry ở lần quét sau,
			// tránh việc file bị loại vĩnh viễn khỏi pipeline chỉ vì sự cố tạm thời.
			log.error("Auto-ingestion failed (transient), moving file back to input for retry. file={}", originalFile, exception);
			moveBackToInput(stagedFile, relativePath);
		}
	}

    // Di chuyển file từ thư mục đầu vào sang thư mục xử lý tạm thời để tránh bị xử lý đồng thời bởi nhiều luồng hoặc bị xử lý khi đang được ghi dữ liệu. Nếu di chuyển không thành công thì ghi log lỗi và trả về null để bỏ qua file này ở lần quét tiếp theo
	private Path moveToProcessing(Path originalFile, Path relativePath) {
		try {
			Path target = resolveProcessingDir().resolve(relativePath).normalize();
			ensureDirectoryExists(target.getParent());
			return moveFile(originalFile, target);
		} catch (NoSuchFileException noSuchFileException) {
			log.warn("Auto-ingestion skipped: file was deleted before it could be moved to processing. file={}", originalFile);
			return null;
		} catch (Exception exception) {
			log.error("Cannot move file to processing area. file={}", originalFile, exception);
			return null;
		}
	}

    // Di chuyển file đã xử lý xong sang thư mục failed để tránh bị xử lý lại, đồng thời ghi log lỗi. Nếu di chuyển không thành công thì ghi log lỗi và giữ nguyên file trong thư mục processing để có thể thử lại ở lần quét tiếp theo
	private void moveToFailed(Path stagedFile, Path relativePath) {
		try {
			Path target = resolveFailedDir().resolve(relativePath).normalize();
			ensureDirectoryExists(target.getParent());
			moveFile(stagedFile, target);
		} catch (Exception exception) {
			log.error("Cannot move file to failed area. file={}", stagedFile, exception);
		}
	}

    // Đưa file từ thư mục xử lý tạm thời trở lại thư mục đầu vào để được retry ở lần quét sau (dùng cho lỗi tạm thời)
	private void moveBackToInput(Path stagedFile, Path relativePath) {
		try {
			Path target = resolveInputDir().resolve(relativePath).normalize();
			ensureDirectoryExists(target.getParent());
			moveFile(stagedFile, target);
		} catch (Exception exception) {
			log.error("Cannot move file back to input area for retry. file={}", stagedFile, exception);
		}
	}

    // Di chuyển file từ source đến target, ưu tiên sử dụng atomic move để tránh tình trạng file bị xử lý khi đang được ghi dữ liệu. Nếu atomic move không được hỗ trợ thì fallback sang move thông thường
	private Path moveFile(Path source, Path target) throws Exception {
		try {
			return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
			return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

    // Thời gian giữa 2 lần quét thư mục để tìm file mới, đơn vị là ms. Mặc định 60000ms (60 giây)
	private long resolvePollerDelayMs() {
		if (appProperties.getAutoIngestion() == null || appProperties.getAutoIngestion().getPollerDelayMs() == null
				|| appProperties.getAutoIngestion().getPollerDelayMs() <= 0) {
			return 60000L;
		}
		return appProperties.getAutoIngestion().getPollerDelayMs();
	}

    // Giới hạn số file được xử lý trong mỗi lần quét thư mục để tránh quá tải. Mặc định 50 file/lần quét
	private int resolveMaxMessagesPerPoll() {
		if (appProperties.getAutoIngestion() == null || appProperties.getAutoIngestion().getMaxMessagesPerPoll() == null
				|| appProperties.getAutoIngestion().getMaxMessagesPerPoll() <= 0) {
			return 50;
		}
		return appProperties.getAutoIngestion().getMaxMessagesPerPoll();
	}

    // Số lần retry tối đa khi đẩy file sang ingestion service (RAG) thất bại. Mặc định 3 nếu để trống hoặc < 0.
    // Đặt 0 để không retry (move thẳng sang .failed).
	private int resolveMaxRetryAttempts() {
		if (appProperties.getAutoIngestion() == null || appProperties.getAutoIngestion().getMaxRetryAttempts() == null
				|| appProperties.getAutoIngestion().getMaxRetryAttempts() < 0) {
			return 3;
		}
		return appProperties.getAutoIngestion().getMaxRetryAttempts();
	}

    // Thời gian tối thiểu mà một file được coi là "ổn định" (không còn đang được ghi thêm dữ liệu) để có thể xử lý, đơn vị là ms. Mặc định 20000ms (20 giây)
	private long resolveStableFileAgeMs() {
		if (appProperties.getAutoIngestion() == null || appProperties.getAutoIngestion().getFileStableSeconds() == null
				|| appProperties.getAutoIngestion().getFileStableSeconds() <= 0) {
			return 20000L;
		}
		return appProperties.getAutoIngestion().getFileStableSeconds() * 1000L;
	}

    // Kiểm tra nếu auto-ingestion được bật trong cấu hình
	private boolean isEnabled() {
		return appProperties.getAutoIngestion() != null && appProperties.getAutoIngestion().isEnabled();
	}

    // Lấy đường dẫn thư mục đầu vào từ cấu hình, nếu không có thì mặc định là "D:/input"
	private Path resolveInputDir() {
		String configured = appProperties.getAutoIngestion() != null ? appProperties.getAutoIngestion().getInputDir() : null;
		if (configured == null || configured.trim().isEmpty()) {
			configured = "D:/input";
		}
		return Paths.get(configured).toAbsolutePath().normalize();
	}

    // Lấy đường dẫn thư mục xử lý tạm thời từ cấu hình, nếu không có thì mặc định là thư mục con ".processing" trong thư mục đầu vào
	private Path resolveProcessingDir() {
		String configured = appProperties.getAutoIngestion() != null ? appProperties.getAutoIngestion().getProcessingDir() : null;
		if (configured == null || configured.trim().isEmpty()) {
			configured = resolveInputDir().resolve(".processing").toString();
		}
		return Paths.get(configured).toAbsolutePath().normalize();
	}

    // Lấy đường dẫn thư mục lưu trữ file bị lỗi từ cấu hình, nếu không có thì mặc định là thư mục con ".failed" trong thư mục đầu vào
	private Path resolveFailedDir() {
		String configured = appProperties.getAutoIngestion() != null ? appProperties.getAutoIngestion().getFailedDir() : null;
		if (configured == null || configured.trim().isEmpty()) {
			configured = resolveInputDir().resolve(".failed").toString();
		}
		return Paths.get(configured).toAbsolutePath().normalize();
	}

    // Đảm bảo thư mục tồn tại, nếu không thì tạo mới. Nếu tạo không thành công thì ném ngoại lệ và ghi log lỗi
	private void ensureDirectoryExists(Path path) {
		if (path == null) {
			return;
		}
		try {
			Files.createDirectories(path);
		} catch (Exception exception) {
			throw new IllegalStateException("Cannot initialize auto-ingestion directories: " + path, exception);
		}
	}

    // Khôi phục các file còn sót lại trong thư mục processing (do app bị crash/restart giữa chừng) trở lại thư mục input
    // để được xử lý lại, tránh file bị kẹt vĩnh viễn trong thư mục processing.
	private void recoverStaleProcessingFiles() {
		Path processingPath = resolveProcessingDir();
		if (processingPath == null || !Files.isDirectory(processingPath)) {
			return;
		}
		try (var stream = Files.walk(processingPath)) {
			stream.filter(Files::isRegularFile)
					.forEach(file -> {
						Path relative = processingPath.relativize(file);
						try {
							Path target = resolveInputDir().resolve(relative).normalize();
							ensureDirectoryExists(target.getParent());
							moveFile(file, target);
							log.warn("Recovered stale processing file back to input: {}", file);
						} catch (Exception exception) {
							log.error("Cannot recover stale processing file. file={}", file, exception);
						}
					});
		} catch (Exception exception) {
			log.error("Cannot scan processing directory for stale files. dir={}", processingPath, exception);
		}
	}

}
