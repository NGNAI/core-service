package ai.service;

import ai.AppProperties;
import ai.dto.outer.ingestion.response.IngestionStatusResponseDto;
import ai.dto.outer.ingestion.response.IngestionUploadResponseDto;
import ai.dto.own.request.NoteBookSourceAddFilesRequestDto;
import ai.dto.own.request.NoteBookSourceAddNotesRequestDto;
import ai.dto.own.request.NoteBookSourceAddTextRequestDto;
import ai.dto.own.response.NoteBookSourceDownloadData;
import ai.dto.own.response.NoteBookSourceJobStatusResponseDto;
import ai.dto.own.response.NoteBookSourcePresignedUrlResponseDto;
import ai.dto.own.response.NoteBookSourceResponseDto;
import ai.entity.postgres.NoteEntity;
import ai.entity.postgres.NoteBookEntity;
import ai.entity.postgres.NoteBookSourceEntity;
import ai.enums.DataIngestionDeleteStatus;
import ai.enums.ApiResponseStatus;
import ai.enums.DataScope;
import ai.enums.SystemEventSource;
import ai.enums.SystemEventType;
import ai.exeption.AppException;
import ai.mapper.NoteBookSourceMapper;
import ai.model.CustomPairModel;
import ai.repository.NoteBookSourceRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class NoteBookSourceService {
    static final String NOTEBOOK_BUCKET = "notebookllm";
    static final int DEFAULT_PRESIGNED_EXPIRY_SECONDS = 900;

    NoteBookSourceRepository noteBookSourceRepository;
    NoteBookSourceMapper noteBookSourceMapper;
    NoteBookService noteBookService;
    NoteService noteService;
    MinioService minioService;
    IngestionService ingestionService;
    SystemEventSseService systemEventSseService;
    OrganizationService organizationService;
    UserService userService;
    AppProperties appProperties;

    /**
     * Lấy danh sách source của notebook theo page. Kết quả trả về bao gồm tổng số lượng source và list source theo page yêu cầu
     * @param noteBookId
     * @param page
     * @param size
     * @return
     */
    public CustomPairModel<Long, List<NoteBookSourceResponseDto>> getSources(UUID noteBookId, int page, int size) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());
        Page<NoteBookSourceEntity> result = noteBookSourceRepository.findByNoteBookId(noteBookId, PageRequest.of(page, size));
        return new CustomPairModel<>(result.getTotalElements(),
                result.getContent().stream().map(noteBookSourceMapper::entityToResponseDto).toList());
    }

    /**
     * Thêm source cho notebook từ file upload. Mỗi file sẽ được tạo thành một source riêng biệt. Trường hợp request có nhiều file, hệ thống sẽ xử lý song song để tối ưu thời gian ingestion. Kết quả trả về là list source đã được tạo tương ứng với các file đã upload.
     * @param noteBookId
     * @param requestDto
     * @return
     */
    public List<NoteBookSourceResponseDto> addFileSources(UUID noteBookId, NoteBookSourceAddFilesRequestDto requestDto) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();
        noteBookService.validateNoteBookOfUser(noteBookId, userId);

        MultipartFile[] files = requestDto == null ? null : requestDto.getFiles();
        List<MultipartFile> validFiles = Arrays.stream(files == null ? new MultipartFile[0] : files)
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (validFiles.isEmpty()) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_PAYLOAD_REQUIRED);
        }

        int poolSize = Math.min(validFiles.size(), Math.max(1, Runtime.getRuntime().availableProcessors()));
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

        try {
            List<CompletableFuture<NoteBookSourceResponseDto>> futures = validFiles.stream()
                    .map(file -> CompletableFuture.supplyAsync(
                        () -> uploadSingleFileAndAttach(noteBookId, file, userId, orgId),
                            executorService))
                    .toList();

            return futures.stream().map(future -> {
                try {
                    return future.join();
                } catch (CompletionException exception) {
                    throw unwrapCompletionException(exception);
                }
            }).toList();
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * Thêm source cho notebook từ text input. Trường displayName là không bắt buộc, nếu người dùng không cung cấp thì hệ thống sẽ tự động tạo displayName theo format "text-source-{8 ký tự random}". Kết quả trả về là source đã được tạo tương ứng với text đã nhập.
     * @param noteBookId
     * @param requestDto
     * @return
     */
    @Transactional
    public NoteBookSourceResponseDto addTextSource(UUID noteBookId, NoteBookSourceAddTextRequestDto requestDto) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();
        noteBookService.validateNoteBookOfUser(noteBookId, userId);

        String textContent = normalizeText(requestDto.getTextContent());
        String textDisplayName = normalizeText(requestDto.getDisplayName());

        String displayName = textDisplayName != null
                ? textDisplayName
                : "text-source-" + UUID.randomUUID().toString().substring(0, 8);

        if (noteBookSourceRepository.existsByNoteBookIdAndDisplayNameAndSourceType(
                noteBookId,
                displayName,
                NoteBookSourceEntity.SourceType.TEXT)) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_ALREADY_EXISTS);
        }

        NoteBookEntity noteBook = noteBookService.getEntityById(noteBookId);
        NoteBookSourceEntity entity = NoteBookSourceEntity.builder()
                .noteBook(noteBook)
            .note(null)
                .sourceType(NoteBookSourceEntity.SourceType.TEXT)
                .displayName(displayName)
                .rawContent(textContent)
                .filePath(null)
                .summary(null)
                .metadata(null)
                .vectorStatus(NoteBookSourceEntity.VectorStatus.NOT_PROCESSED)
                .jobId(null)
                .ownerId(userId)
                .organizationId(orgId)
                .deleteStatus(DataIngestionDeleteStatus.ACTIVE)
                .build();

        NoteBookSourceEntity savedEntity = noteBookSourceRepository.save(entity);
        return dispatchSourceForIngestion(savedEntity);
    }

    /**
     * Thêm source cho notebook từ các note đã có. Mỗi note sẽ được tạo thành một source riêng biệt. Kết quả trả về là list source đã được tạo tương ứng với các note đã chọn.
     * @param noteBookId
     * @param requestDto
     * @return
     */
    @Transactional
    public List<NoteBookSourceResponseDto> addNoteSources(UUID noteBookId, NoteBookSourceAddNotesRequestDto requestDto) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();
        noteBookService.validateNoteBookOfUser(noteBookId, userId);

        return requestDto.getNoteIds().stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
            .map(noteId -> createNoteSource(noteBookId, noteId, userId, orgId))
                .toList();
    }

    /**
     * Tạo source cho notebook từ note đã có. Kết quả trả về là source đã được tạo tương ứng với note đã chọn.
     * @param noteBookId
     * @param noteId
     * @param userId
     * @param orgId
     * @return
     */
    @Transactional
        protected NoteBookSourceResponseDto createNoteSource(UUID noteBookId, UUID noteId, UUID userId, UUID orgId) {
        noteBookService.validateNoteBookOfUser(noteBookId, userId);
        noteService.validateNoteOfUser(noteId, userId);

        if (noteBookSourceRepository.existsByNoteBookIdAndNote_IdAndSourceType(
                noteBookId,
                noteId,
                NoteBookSourceEntity.SourceType.NOTE)) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_ALREADY_EXISTS);
        }

        NoteEntity note = noteService.getEntityById(noteId);
        String displayName = normalizeText(note.getTitle());
        if (displayName == null) {
            displayName = "note-source-" + note.getId().toString().substring(0, 8);
        }

        NoteBookEntity noteBook = noteBookService.getEntityById(noteBookId);
        NoteBookSourceEntity entity = NoteBookSourceEntity.builder()
                .noteBook(noteBook)
                .note(note)
                .sourceType(NoteBookSourceEntity.SourceType.NOTE)
                .displayName(displayName)
                .rawContent(note.getContent())
                .filePath(null)
                .summary(null)
                .metadata(null)
                .vectorStatus(NoteBookSourceEntity.VectorStatus.NOT_PROCESSED)
                .jobId(null)
                .ownerId(userId)
                .organizationId(orgId)
                .deleteStatus(DataIngestionDeleteStatus.ACTIVE)
                .build();

        NoteBookSourceEntity savedEntity = noteBookSourceRepository.save(entity);
        return dispatchSourceForIngestion(savedEntity);
    }

    /**
     * Xóa source của notebook. Thực chất là đánh dấu source là PENDING_DELETE để hệ thống xử lý xóa sau.
     * @param noteBookId
     * @param sourceId
     */
    @Transactional
    public void removeSource(UUID noteBookId, UUID sourceId) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());
        NoteBookSourceEntity entity = getSourceEntity(noteBookId, sourceId);
        queueDelete(entity);
    }

    /**
     * Tải xuống source của notebook.
     * @param noteBookId
     * @param sourceId
     * @return
     */
    public NoteBookSourceDownloadData downloadSource(UUID noteBookId, UUID sourceId) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());
        NoteBookSourceEntity source = getSourceEntity(noteBookId, sourceId);
        validateDownloadableSource(source);

        MinioService.MinioObjectData objectData = minioService.download(source.getFilePath(), NOTEBOOK_BUCKET);
        return new NoteBookSourceDownloadData(resolveFileName(source), objectData.getContentType(), objectData.getBytes());
    }

    /**
     * Lấy URL tải xuống có thời hạn của source notebook. URL này được tạo bởi MinIO và sẽ hết hạn sau một khoảng thời gian nhất định (mặc định là 15 phút). Trường expiresInSeconds cho phép người dùng tùy chỉnh thời gian hết hạn của URL, nếu không cung cấp hoặc cung cấp giá trị không hợp lệ thì sẽ sử dụng giá trị mặc định.
     * @param noteBookId
     * @param sourceId
     * @param expiresInSeconds
     * @return
     */
    public NoteBookSourcePresignedUrlResponseDto getSourceDownloadUrl(UUID noteBookId, UUID sourceId, Integer expiresInSeconds) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());
        NoteBookSourceEntity source = getSourceEntity(noteBookId, sourceId);
        validateDownloadableSource(source);

        int effectiveExpiry = expiresInSeconds == null || expiresInSeconds <= 0
                ? DEFAULT_PRESIGNED_EXPIRY_SECONDS
                : expiresInSeconds;

        String url = minioService.generatePresignedDownloadUrl(source.getFilePath(), effectiveExpiry, NOTEBOOK_BUCKET);
        return NoteBookSourcePresignedUrlResponseDto.builder()
                .url(url)
                .expiresInSeconds(effectiveExpiry)
                .build();
    }

    /**
     * Đồng bộ trạng thái vector của các source notebook đang chờ xử lý hoặc đang xử lý trên ingestion service. Phương thức này thường được gọi định kỳ bởi scheduler để đảm bảo trạng thái vector của các source notebook luôn được cập nhật kịp thời và chính xác, đặc biệt là những source có jobId đã được gửi lên ingestion service nhưng chưa có callback về hoặc có callback về nhưng trạng thái vẫn là processing.
     */
    public void syncPendingVectorStatuses() {
        System.out.println("Start syncing notebook source vector statuses...");
        noteBookSourceRepository.findSourcesForIngestionMaintenance().forEach(source -> {
            try {
                if (!DataIngestionDeleteStatus.ACTIVE.equals(resolveDeleteStatus(source))) {
                    return;
                }

                if (source.getJobId() == null) {
                    dispatchSourceForIngestion(source);
                    return;
                }

                IngestionStatusResponseDto statusResponse = ingestionService.getJobStatus(source.getJobId());
                updateStatusAndBuildResponse(source, statusResponse, true);
            } catch (Exception exception) {
                System.err.println("Error syncing notebook source with ID: " + source.getId());
                exception.printStackTrace();
            }
        });
        System.out.println("Finished syncing notebook source vector statuses.");
    }

    /**
     * Xử lý hàng đợi xóa các source notebook. Phương thức này thường được gọi định kỳ bởi scheduler để thực hiện xóa các source notebook đã được đánh dấu là PENDING_DELETE nhưng chưa được xóa ngay do có thể đang chờ xử lý trên ingestion service hoặc đang trong quá trình xóa mà gặp lỗi cần retry. Việc xử lý hàng đợi xóa định kỳ giúp đảm bảo các source notebook không còn cần thiết sẽ được xóa sạch sẽ khỏi hệ thống, đồng thời giải phóng tài nguyên lưu trữ và tránh nhầm lẫn cho người dùng khi nhìn thấy các source đã bị xóa nhưng vẫn còn hiển thị trong giao diện.
     */
    public void processPendingDeleteQueue() {
        System.out.println("Start processing pending notebook source deletions...");
        noteBookSourceRepository.findByDeleteStatusIn(List.of(
                DataIngestionDeleteStatus.PENDING_DELETE,
                DataIngestionDeleteStatus.DELETE_FAILED)).forEach(source -> {
            try {
                executeDelete(source);
            } catch (Exception exception) {
                System.err.println("Error processing delete for notebook source with ID: " + source.getId());
                exception.printStackTrace();
            }
        });
        System.out.println("Finished processing pending notebook source deletions.");
    }

    /**
     * Gửi source notebook lên ingestion service để xử lý embedding. Phương thức này được gọi khi tạo mới source notebook hoặc khi đồng bộ trạng thái vector của các source notebook đang chờ xử lý trên ingestion service nhưng chưa có callback về. Kết quả trả về là source notebook đã được cập nhật trạng thái vector tương ứng với kết quả xử lý từ ingestion service (processing, completed hoặc error).
     * @param source
     * @return
     */
    @Transactional(noRollbackFor = AppException.class)
    public NoteBookSourceResponseDto dispatchSourceForIngestion(NoteBookSourceEntity source) {
        if (source == null) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_NOT_EXISTS);
        }

        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(source))) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_DELETE_IN_PROGRESS);
        }

        try {
            byte[] payloadBytes = resolvePayloadBytes(source);
            String fileName = resolvePayloadFileName(source);
            String callbackUrl = resolveCallbackUrl();

            String userName = resolveUserName(source);
            String unitId = source.getOrganizationId() != null
                    ? source.getOrganizationId().toString()
                    : source.getNoteBook().getId().toString();
            String unitName = resolveUnitName(source);

            IngestionUploadResponseDto ingestionResponse = ingestionService.pushToVector(
                    payloadBytes,
                    fileName,
                    source.getId().toString(),
                    userName,
                    unitId,
                    unitName,
                    DataScope.LOCAL,
                    callbackUrl);

            if (ingestionResponse == null || ingestionResponse.getJobId() == null) {
                source.setVectorStatus(NoteBookSourceEntity.VectorStatus.ERROR);
                source = noteBookSourceRepository.save(source);
                publishStatusEvent(source, NoteBookSourceEntity.VectorStatus.ERROR);
                return noteBookSourceMapper.entityToResponseDto(source);
            }

            source.setJobId(ingestionResponse.getJobId());
            source.setVectorStatus(NoteBookSourceEntity.VectorStatus.PROCESSING);
            source = noteBookSourceRepository.save(source);
            publishStatusEvent(source, NoteBookSourceEntity.VectorStatus.PROCESSING);
            return noteBookSourceMapper.entityToResponseDto(source);
        } catch (Exception exception) {
            source.setVectorStatus(NoteBookSourceEntity.VectorStatus.ERROR);
            source = noteBookSourceRepository.save(source);
            publishStatusEvent(source, NoteBookSourceEntity.VectorStatus.ERROR);
            return noteBookSourceMapper.entityToResponseDto(source);
        }
    }

    /**
     * Poll trạng thái xử lý ingestion job bằng jobId trả về từ phương thức pushToVector. Thông thường sẽ cần gọi phương thức này nhiều lần sau khi gọi pushToVector để theo dõi tiến độ xử lý của ingestion job, cho đến khi trạng thái trả về là success hoặc failed thì thôi
     * @param noteBookId
     * @param sourceId
     * @return
     */
    @Transactional
    public NoteBookSourceJobStatusResponseDto pollIngestionJobStatus(UUID noteBookId, UUID sourceId) {
        NoteBookSourceEntity source = noteBookSourceRepository.findByNoteBookIdAndId(noteBookId, sourceId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_NOT_EXISTS));

        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(source))) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_DELETE_IN_PROGRESS);
        }

        if (source.getJobId() == null) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_JOB_ID_NOT_EXISTS);
        }

        IngestionStatusResponseDto statusResponse = ingestionService.getJobStatus(source.getJobId());
        return updateStatusAndBuildResponse(source, statusResponse, true);
    }

    /**
     * Xử lý callback trạng thái từ ingestion service sau khi hoàn thành xử lý embedding cho source notebook. Phương thức này sẽ được gọi bởi ingestion service thông qua callback URL đã đăng ký khi gửi source lên ingestion service. Kết quả trả về là source notebook đã được cập nhật trạng thái vector tương ứng với kết quả xử lý từ ingestion service (completed hoặc error).
     * @param callbackDto
     * @return
     */
    @Transactional
    public NoteBookSourceJobStatusResponseDto handleIngestionCallback(IngestionStatusResponseDto callbackDto) {
        if (callbackDto == null) {
            throw new AppException(ApiResponseStatus.INVALID_REQUEST_INFORMATION);
        }

        NoteBookSourceEntity source;
        if (callbackDto.getMeta() != null && callbackDto.getMeta().getFile_id() != null) {
            source = noteBookSourceRepository.findById(callbackDto.getMeta().getFile_id())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_NOT_EXISTS));
        } else if (callbackDto.getJobId() != null) {
            source = noteBookSourceRepository.findByJobId(callbackDto.getJobId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_NOT_EXISTS));
        } else {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_JOB_ID_NOT_EXISTS);
        }

        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(source))) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_DELETE_IN_PROGRESS);
        }

        return updateStatusAndBuildResponse(source, callbackDto, true);
    }

    /**
     * Xóa source của notebook. Thực chất là đánh dấu source là PENDING_DELETE để hệ thống xử lý xóa sau.
     * @param source
     * @return
     */
    @Transactional
    public NoteBookSourceResponseDto queueDelete(NoteBookSourceEntity source) {
        if (source == null) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_NOT_EXISTS);
        }

        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(source))) {
            return noteBookSourceMapper.entityToResponseDto(source);
        }

        source.setDeleteStatus(DataIngestionDeleteStatus.PENDING_DELETE);
        source = noteBookSourceRepository.save(source);

        publishDeleteEvent(source, SystemEventType.NOTEBOOK_SOURCE_DELETE_QUEUED, noteBookSourceMapper.entityToResponseDto(source));

        try {
            executeDelete(source);
        } catch (Exception exception) {
            System.err.println("Immediate delete failed for notebook source with ID: " + source.getId());
            exception.printStackTrace();
        }

        return noteBookSourceMapper.entityToResponseDto(source);
    }

    /**
     * Thực hiện xóa source của notebook. Phương thức này sẽ được gọi bởi processPendingDeleteQueue để thực hiện xóa các source notebook đã được đánh dấu là PENDING_DELETE nhưng chưa được xóa ngay do có thể đang chờ xử lý trên ingestion service hoặc đang trong quá trình xóa mà gặp lỗi cần retry. Việc xử lý hàng đợi xóa định kỳ giúp đảm bảo các source notebook không còn cần thiết sẽ được xóa sạch sẽ khỏi hệ thống, đồng thời giải phóng tài nguyên lưu trữ và tránh nhầm lẫn cho người dùng khi nhìn thấy các source đã bị xóa nhưng vẫn còn hiển thị trong giao diện.
     * @param source
     */
    private void executeDelete(NoteBookSourceEntity source) {
        NoteBookSourceResponseDto deletingData = noteBookSourceMapper.entityToResponseDto(source);

        try {
            if (NoteBookSourceEntity.SourceType.FILE.equals(source.getSourceType())
                    && source.getFilePath() != null
                    && !source.getFilePath().isBlank()) {
                minioService.delete(source.getFilePath(), NOTEBOOK_BUCKET);
            }

            if (source.getJobId() != null) {
                ingestionService.deleteFile(source.getId().toString());
            }

            noteBookSourceRepository.delete(source);
            publishDeleteEvent(source, SystemEventType.NOTEBOOK_SOURCE_DELETED, deletingData);
        } catch (Exception exception) {
            noteBookSourceRepository.findById(source.getId()).ifPresent(entity -> {
                entity.setDeleteStatus(DataIngestionDeleteStatus.DELETE_FAILED);
                NoteBookSourceEntity savedEntity = noteBookSourceRepository.save(entity);
                publishDeleteEvent(savedEntity, SystemEventType.NOTEBOOK_SOURCE_DELETE_FAILED, noteBookSourceMapper.entityToResponseDto(savedEntity));
            });
            throw exception;
        }
    }

    /**
     * Cập nhật trạng thái vector của source notebook dựa trên phản hồi trạng thái từ ingestion service, đồng thời xây dựng response DTO để trả về cho client. Phương thức này sẽ được gọi sau khi nhận được phản hồi trạng thái từ ingestion service thông qua phương thức pollIngestionJobStatus hoặc handleIngestionCallback. Nếu trạng thái vector của source notebook có sự thay đổi so với trước đó, hệ thống sẽ cập nhật lại trong database và phát đi sự kiện cập nhật trạng thái để client có thể nhận biết được sự thay đổi này.
     * @param source
     * @param ingestionStatusResponse
     * @param emitEvent
     * @return
     */
    private NoteBookSourceJobStatusResponseDto updateStatusAndBuildResponse(
            NoteBookSourceEntity source,
            IngestionStatusResponseDto ingestionStatusResponse,
            boolean emitEvent) {
        NoteBookSourceEntity.VectorStatus resolvedStatus = resolveVectorStatus(
                ingestionStatusResponse == null ? null : ingestionStatusResponse.getStatus(),
                source.getVectorStatus());

        if (source.getVectorStatus() == null || !resolvedStatus.equals(source.getVectorStatus())) {
            source.setVectorStatus(resolvedStatus);

            if (ingestionStatusResponse != null
                    && ingestionStatusResponse.getMeta() != null
                    && ingestionStatusResponse.getMeta().getUnit_id() != null) {
                source.setOrganizationId(ingestionStatusResponse.getMeta().getUnit_id());
            }

            source = noteBookSourceRepository.save(source);
        }

        if (emitEvent) {
            publishStatusEvent(source, resolvedStatus);
        }

        return NoteBookSourceJobStatusResponseDto.builder()
                .sourceId(source.getId())
                .noteBookId(source.getNoteBook() == null ? null : source.getNoteBook().getId())
                .jobId(source.getJobId())
                .vectorStatus(resolvedStatus.name())
                .message(ingestionStatusResponse == null ? null : ingestionStatusResponse.getMessage())
                .build();
    }

    /**
     * Giải mã payload của source notebook thành mảng byte để gửi lên ingestion service. Đối với source có kiểu FILE, hệ thống sẽ tải file từ MinIO dựa trên filePath đã lưu trong database, sau đó đọc nội dung file và trả về dưới dạng mảng byte. Đối với source có kiểu TEXT hoặc NOTE, hệ thống sẽ lấy rawContent đã lưu trong database, chuyển đổi sang mảng byte bằng encoding UTF-8 và trả về. Nếu source không có payload hợp lệ (ví dụ: thiếu filePath đối với FILE hoặc thiếu rawContent đối với TEXT/NOTE), hệ thống sẽ ném ra ngoại lệ để thông báo lỗi.
     * @param source
     * @return
     */
    private byte[] resolvePayloadBytes(NoteBookSourceEntity source) {
        if (source.getSourceType() == null) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_NOT_EXISTS);
        }

        if (NoteBookSourceEntity.SourceType.FILE.equals(source.getSourceType())) {
            if (source.getFilePath() == null || source.getFilePath().isBlank()) {
                throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_NOT_EXISTS);
            }
            MinioService.MinioObjectData objectData = minioService.download(source.getFilePath(), NOTEBOOK_BUCKET);
            return objectData.getBytes();
        }

        if (source.getRawContent() == null || source.getRawContent().isBlank()) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_NOT_EXISTS);
        }

        return source.getRawContent().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Giải mã tên file để sử dụng làm fileName khi gửi payload lên ingestion service hoặc khi trả về cho client khi tải xuống source notebook. Đối với source có kiểu FILE, nếu trường displayName có giá trị hợp lệ thì sẽ sử dụng displayName làm fileName, ngược lại sẽ sử dụng id của source cộng với đuôi .bin làm fileName. Đối với source có kiểu TEXT hoặc NOTE, nếu trường displayName có giá trị hợp lệ thì sẽ sử dụng displayName làm base name, sau đó thêm đuôi .txt nếu base name chưa có đuôi này, ngược lại sẽ sử dụng id của source cộng với đuôi .txt làm fileName.
     * @param source
     * @return
     */
    private String resolvePayloadFileName(NoteBookSourceEntity source) {
        if (NoteBookSourceEntity.SourceType.FILE.equals(source.getSourceType())) {
            if (source.getDisplayName() != null && !source.getDisplayName().isBlank()) {
                return source.getDisplayName();
            }
            return source.getId().toString() + ".bin";
        }

        String baseName = (source.getDisplayName() == null || source.getDisplayName().isBlank())
                ? source.getId().toString()
                : source.getDisplayName().trim();
        return baseName.endsWith(".txt") ? baseName : baseName + ".txt";
    }

    /**
     * Giải mã trạng thái vector trả về từ ingestion service thành enum VectorStatus của hệ thống. Phương thức này sẽ được sử dụng để chuyển đổi trạng thái vector nhận được từ ingestion service (thường là chuỗi như "processing", "completed", "error", v.v.) thành enum VectorStatus tương ứng trong hệ thống (PROCESSING, PROCESSED, ERROR, NOT_PROCESSED). Nếu trạng thái nhận được không hợp lệ hoặc không khớp với bất kỳ giá trị nào đã định nghĩa, phương thức sẽ trả về fallbackStatus nếu có hoặc mặc định là PROCESSING để đảm bảo hệ thống vẫn hoạt động ổn định mà không bị lỗi do trạng thái không hợp lệ.
     * @param rawStatus
     * @param fallbackStatus
     * @return
     */
    private NoteBookSourceEntity.VectorStatus resolveVectorStatus(
            String rawStatus,
            NoteBookSourceEntity.VectorStatus fallbackStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return fallbackStatus == null ? NoteBookSourceEntity.VectorStatus.PROCESSING : fallbackStatus;
        }

        String normalized = rawStatus.trim().toUpperCase();
        if ("SUCCESS".equals(normalized) || "DONE".equals(normalized) || "COMPLETED".equals(normalized) || "PROCESSED".equals(normalized)) {
            return NoteBookSourceEntity.VectorStatus.PROCESSED;
        }

        if ("ERROR".equals(normalized) || "FAILED".equals(normalized)) {
            return NoteBookSourceEntity.VectorStatus.ERROR;
        }

        if ("NOT_PROCESSED".equals(normalized)) {
            return NoteBookSourceEntity.VectorStatus.NOT_PROCESSED;
        }

        return NoteBookSourceEntity.VectorStatus.PROCESSING;
    }

    /**
     * Phát đi sự kiện cập nhật trạng thái vector của source notebook để client có thể nhận biết được sự thay đổi này. Phương thức này sẽ được gọi sau khi cập nhật trạng thái vector của source notebook trong database, nếu emitEvent là true thì sẽ phát đi sự kiện với thông tin về source notebook đã được cập nhật trạng thái vector, ngược lại sẽ không phát đi sự kiện nào. Sự kiện được phát đi sẽ bao gồm organizationId, ownerId, eventType (được xác định dựa trên trạng thái vector mới), source (NOTEBOOK_SOURCE) và payload là thông tin chi tiết của source notebook đã được cập nhật.
     * @param source
     * @param vectorStatus
     */
    private void publishStatusEvent(NoteBookSourceEntity source, NoteBookSourceEntity.VectorStatus vectorStatus) {
        if (source == null
                || source.getOwnerId() == null
                || source.getOrganizationId() == null) {
            return;
        }

        systemEventSseService.publish(
                source.getOrganizationId(),
                source.getOwnerId(),
                resolveStatusEventType(vectorStatus),
                SystemEventSource.NOTEBOOK_SOURCE,
                noteBookSourceMapper.entityToResponseDto(source));
    }

    /**
     * Phát đi sự kiện liên quan đến xóa source notebook để client có thể nhận biết được các sự kiện này. Phương thức này sẽ được gọi sau khi đánh dấu source notebook là PENDING_DELETE hoặc sau khi thực hiện xóa source notebook, với eventType tương ứng là NOTEBOOK_SOURCE_DELETE_QUEUED hoặc NOTEBOOK_SOURCE_DELETED. Sự kiện được phát đi sẽ bao gồm organizationId, ownerId, eventType (được xác định dựa trên hành động xóa), source (NOTEBOOK_SOURCE) và payload là thông tin chi tiết của source notebook đã được đánh dấu xóa hoặc đã bị xóa.
     * @param source
     * @param type
     * @param payload
     */
    private void publishDeleteEvent(NoteBookSourceEntity source, SystemEventType type, Object payload) {
        if (source == null
                || source.getOwnerId() == null
                || source.getOrganizationId() == null) {
            return;
        }

        systemEventSseService.publish(
                source.getOrganizationId(),
                source.getOwnerId(),
                type,
                SystemEventSource.NOTEBOOK_SOURCE,
                payload);
    }

    /**
     * Xác định eventType của sự kiện cập nhật trạng thái vector dựa trên giá trị của vectorStatus. Nếu vectorStatus là PROCESSED thì eventType sẽ là NOTEBOOK_SOURCE_COMPLETED, nếu vectorStatus là ERROR thì eventType sẽ là NOTEBOOK_SOURCE_FAILED, ngược lại sẽ là NOTEBOOK_SOURCE_STATUS_UPDATED. Việc xác định eventType chính xác giúp client có thể nhận biết được ý nghĩa của sự kiện và cập nhật giao diện người dùng một cách phù hợp.
     * @param vectorStatus
     * @return
     */
    private SystemEventType resolveStatusEventType(NoteBookSourceEntity.VectorStatus vectorStatus) {
        if (vectorStatus == null) {
            return SystemEventType.NOTEBOOK_SOURCE_STATUS_UPDATED;
        }

        if (NoteBookSourceEntity.VectorStatus.PROCESSED.equals(vectorStatus)) {
            return SystemEventType.NOTEBOOK_SOURCE_COMPLETED;
        }

        if (NoteBookSourceEntity.VectorStatus.ERROR.equals(vectorStatus)) {
            return SystemEventType.NOTEBOOK_SOURCE_FAILED;
        }

        return SystemEventType.NOTEBOOK_SOURCE_STATUS_UPDATED;
    }

    /**
     * Giải mã trạng thái xóa của source notebook. Phương thức này sẽ được sử dụng để chuyển đổi trạng thái xóa nhận được từ database (có thể là null, ACTIVE, PENDING_DELETE hoặc DELETE_FAILED) thành enum DataIngestionDeleteStatus tương ứng trong hệ thống. Nếu trạng thái nhận được là null thì phương thức sẽ trả về ACTIVE để đảm bảo hệ thống vẫn hoạt động ổn định mà không bị lỗi do trạng thái null.
     * @param source
     * @return
     */
    private DataIngestionDeleteStatus resolveDeleteStatus(NoteBookSourceEntity source) {
        return source.getDeleteStatus() == null ? DataIngestionDeleteStatus.ACTIVE : source.getDeleteStatus();
    }

    /**
     * Giải mã tên đơn vị (unit name) để sử dụng làm thông tin metadata khi gửi payload lên ingestion service. Đối với source notebook có organizationId hợp lệ thì sẽ lấy tên tổ chức tương ứng từ OrganizationService, ngược lại sẽ sử dụng tên notebook nếu có hoặc fallback sang "notebook" nếu không có tên notebook. Việc giải mã tên đơn vị chính xác giúp cung cấp thông tin đầy đủ và chính xác cho ingestion service, từ đó hỗ trợ việc quản lý và phân loại các nguồn dữ liệu một cách hiệu quả hơn.
     * @param source
     * @return
     */
    private String resolveUnitName(NoteBookSourceEntity source) {
        UUID orgId = source.getOrganizationId();
        if (orgId != null) {
            try {
                return organizationService.getEntityById(orgId).getName();
            } catch (Exception ignored) {
                // fallback below
            }
        }

        return source.getNoteBook() == null ? "notebook" : source.getNoteBook().getTitle();
    }

    /**
     * Giải mã callback URL để sử dụng khi gửi payload lên ingestion service. Hệ thống sẽ ưu tiên sử dụng URL được cấu hình riêng cho notebook source callback nếu có, ngược lại sẽ fallback sang URL chung cho data ingestion callback nếu có, nếu cả hai đều không có thì sẽ trả về null. Việc giải mã callback URL chính xác giúp đảm bảo rằng ingestion service có thể gửi phản hồi trạng thái về đúng endpoint đã đăng ký, từ đó hệ thống có thể cập nhật trạng thái vector của source notebook một cách kịp thời và chính xác.
     * @return
     */
    private String resolveCallbackUrl() {
        if (appProperties.getIntegration() != null
                && appProperties.getIntegration().getNotebookSourceCallback() != null
                && appProperties.getIntegration().getNotebookSourceCallback().getUrl() != null
                && !appProperties.getIntegration().getNotebookSourceCallback().getUrl().trim().isEmpty()) {
            return appProperties.getIntegration().getNotebookSourceCallback().getUrl().trim();
        }
        return null;
    }

    /**
     * Giải mã tên người dùng để sử dụng làm thông tin metadata khi gửi payload lên ingestion service. Hệ thống sẽ lấy ownerId từ source notebook, sau đó truy vấn UserService để lấy thông tin người dùng tương ứng và trả về userName. Nếu không lấy được ownerId hoặc không tìm thấy người dùng tương ứng thì sẽ fallback sang "notebook-user". Việc giải mã tên người dùng chính xác giúp cung cấp thông tin đầy đủ và chính xác cho ingestion service, từ đó hỗ trợ việc quản lý và phân loại các nguồn dữ liệu một cách hiệu quả hơn.
     * @param source
     * @return
     */
    private String resolveUserName(NoteBookSourceEntity source) {
        if (source.getOwnerId() == null) {
            return "notebook-user";
        }

        try {
            return userService.getEntityById(source.getOwnerId()).getUserName();
        } catch (Exception ignored) {
            return "notebook-user";
        }
    }

    /**
     * Giải mã ngoại lệ CompletionException để lấy ra nguyên nhân gốc (root cause) và trả về dưới dạng RuntimeException. Phương thức này sẽ được sử dụng để xử lý các ngoại lệ phát sinh trong quá trình gọi các phương thức bất đồng bộ (asynchronous) có thể ném ra CompletionException, nhằm mục đích lấy ra nguyên nhân gốc của lỗi để có thể xử lý hoặc thông báo lỗi một cách chính xác hơn. Nếu nguyên nhân gốc là một RuntimeException thì sẽ trả về nguyên nhân đó, ngược lại sẽ trả về một AppException với status UNEXPECTED để đảm bảo hệ thống vẫn hoạt động ổn định mà không bị lỗi do ngoại lệ không mong muốn.
     * @param exception
     * @return
     */
    private RuntimeException unwrapCompletionException(CompletionException exception) {
        Throwable cause = exception;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }

        return new AppException(ApiResponseStatus.UNEXPECTED);
    }

    /**
     * Xử lý upload một file và tạo source notebook tương ứng. Phương thức này sẽ được gọi khi người dùng muốn thêm một file làm nguồn dữ liệu cho notebook. Hệ thống sẽ thực hiện upload file lên MinIO, sau đó tạo một bản ghi NoteBookSourceEntity với thông tin liên quan đến file đã upload, cuối cùng gửi source notebook mới tạo lên ingestion service để xử lý embedding. Kết quả trả về là response DTO chứa thông tin chi tiết của source notebook đã được tạo và gửi lên ingestion service.
     * @param noteBookId
     * @param file
     * @param userId
     * @param orgId
     * @return
     */
    private NoteBookSourceResponseDto uploadSingleFileAndAttach(UUID noteBookId, MultipartFile file, UUID userId, UUID orgId) {
        String originalName = file.getOriginalFilename();
        String displayName = (originalName == null || originalName.isBlank())
                ? "unnamed-source"
                : originalName;

        if (noteBookSourceRepository.existsByNoteBookIdAndDisplayNameAndSourceType(
                noteBookId,
                displayName,
                NoteBookSourceEntity.SourceType.FILE)) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_ALREADY_EXISTS);
        }

        String objectPath = minioService.upload(
                file,
                userId.toString(),
                noteBookId.toString(),
                NOTEBOOK_BUCKET);

        NoteBookEntity noteBook = noteBookService.getEntityById(noteBookId);
        NoteBookSourceEntity entity = NoteBookSourceEntity.builder()
                .noteBook(noteBook)
                .note(null)
                .sourceType(NoteBookSourceEntity.SourceType.FILE)
                .displayName(displayName)
                .rawContent(null)
                .filePath(objectPath)
                .summary(null)
                .metadata(null)
                .vectorStatus(NoteBookSourceEntity.VectorStatus.NOT_PROCESSED)
                .jobId(null)
                .ownerId(userId)
                .organizationId(orgId)
                .deleteStatus(DataIngestionDeleteStatus.ACTIVE)
                .build();

        NoteBookSourceEntity savedEntity = noteBookSourceRepository.save(entity);
        return dispatchSourceForIngestion(savedEntity);
    }

    /**
     * Lấy entity của source notebook dựa trên noteBookId và sourceId, đồng thời kiểm tra xem source notebook có thuộc về notebook tương ứng với noteBookId hay không. Nếu không tìm thấy source notebook
     * @param noteBookId
     * @param sourceId
     * @return
     */
    private NoteBookSourceEntity getSourceEntity(UUID noteBookId, UUID sourceId) {
        return noteBookSourceRepository.findByNoteBookIdAndId(noteBookId, sourceId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_NOT_EXISTS));
    }

    /**
     * Kiểm tra xem source notebook có đủ điều kiện để tải xuống hay không. Đối với source có kiểu FILE, hệ thống sẽ kiểm tra xem filePath đã tồn tại và hợp lệ hay chưa, nếu không
     * @param source
     */
    private void validateDownloadableSource(NoteBookSourceEntity source) {
        if (!NoteBookSourceEntity.SourceType.FILE.equals(source.getSourceType())
                || source.getFilePath() == null
                || source.getFilePath().isBlank()) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_NOT_EXISTS);
        }
    }

    /**
     * Giải mã tên file để sử dụng làm fileName khi trả về cho client khi tải xuống source notebook. Đối với source có kiểu FILE, nếu trường displayName có giá trị hợp lệ thì sẽ sử dụng displayName làm fileName, ngược lại sẽ sử dụng tên file gốc từ filePath nếu có hoặc fallback sang id của source nếu không có tên file gốc. Việc giải mã tên file chính xác giúp cung cấp trải nghiệm người dùng tốt hơn khi tải xuống source notebook, thay vì chỉ nhận được một tên file mặc định không có ý nghĩa.
     * @param source
     * @return
     */
    private String resolveFileName(NoteBookSourceEntity source) {
        if (source.getDisplayName() != null && !source.getDisplayName().isBlank()) {
            return source.getDisplayName();
        }

        Path filePath = Path.of(source.getFilePath());
        Path fileName = filePath.getFileName();
        return fileName == null ? source.getId().toString() : fileName.toString();
    }

    /**
     * Chuẩn hóa chuỗi văn bản bằng cách loại bỏ khoảng trắng ở đầu và cuối, nếu sau khi loại bỏ khoảng trắng mà chuỗi trở nên rỗng thì sẽ trả về null. Phương thức này sẽ được sử dụng để đảm bảo rằng các trường văn bản như displayName, summary, metadata, v.v. được lưu trữ trong database một cách nhất quán và không chứa các giá trị không mong muốn như chuỗi rỗng hoặc chỉ chứa khoảng trắng.
     * @param value
     * @return
     */
    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
