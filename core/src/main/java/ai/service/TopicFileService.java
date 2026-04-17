package ai.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ai.dto.own.request.DataIngestionUploadRequestDto;
import ai.dto.own.response.TopicFileResponseDto;
import ai.entity.postgres.DataIngestionEntity;
import ai.entity.postgres.TopicEntity;
import ai.entity.postgres.TopicFileEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.DataScope;
import ai.enums.DataSource;
import ai.exeption.AppException;
import ai.mapper.TopicFileMapper;
import ai.repository.TopicFileRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class TopicFileService {
    TopicFileRepository topicFileRepository;
    TopicFileMapper topicFileMapper;
    TopicService topicService;
    DataIngestionService dataIngestionService;

    /**
     * Lấy danh sách file của một topic với phân trang. Phương thức này sẽ kiểm tra quyền truy cập của người dùng trước khi trả về kết quả.
     * @param topicId
     * @param page
     * @param size
     * @return
     */
    public Pair<Long, List<TopicFileResponseDto>> getFiles(UUID topicId, int page, int size) {
        topicService.validateTopicOfUser(topicId, JwtUtil.getUserId());
        Page<TopicFileEntity> result = topicFileRepository.findByTopicId(topicId, PageRequest.of(page, size));
        return Pair.of(result.getTotalElements(),
                result.getContent().stream().map(topicFileMapper::entityToResponseDto).toList());
    }

    /**
     * Xóa một file đã được đính kèm khỏi topic. Phương thức này sẽ kiểm tra quyền truy cập của người dùng trước khi thực hiện xóa.
     * @param topicId ID của topic
     * @param files Mảng các file cần xóa
     */
    public List<TopicFileResponseDto> uploadFilesAndWaitForCompletion(UUID topicId, MultipartFile[] files) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        topicService.validateTopicOfUser(topicId, userId);

        List<MultipartFile> validFiles = Arrays.stream(files == null ? new MultipartFile[0] : files)
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (validFiles.isEmpty()) {
            return List.of();
        }

        int poolSize = Math.min(validFiles.size(), Math.max(1, Runtime.getRuntime().availableProcessors()));
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

        try {
            List<CompletableFuture<TopicFileResponseDto>> futures = validFiles.stream()
                    .map(file -> CompletableFuture.supplyAsync(
                            () -> uploadSingleFileAndAttach(topicId, file, userId, orgId),
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
     * Đính kèm một file đã được ingest vào topic. Phương thức này sẽ kiểm tra quyền truy cập của người dùng và trạng thái của data ingestion trước khi thực hiện đính kèm.
     * @param topicId ID của topic
     * @param dataIngestionId ID của data ingestion
     * @param summary Tóm tắt của file
     * @param metadata Metadata của file
     * @param userId ID của người dùng
     * @return
     */
    @Transactional
    public TopicFileResponseDto addFile(UUID topicId, UUID fileId, String summary, String metadata, UUID userId) {
        topicService.validateTopicOfUser(topicId, userId);

        if (topicFileRepository.existsByTopicIdAndDataIngestionId(topicId, fileId)) {
            throw new AppException(ApiResponseStatus.TOPIC_FILE_ALREADY_EXISTS);
        }

        TopicEntity topic = topicService.getEntityById(topicId);
        DataIngestionEntity dataIngestion = dataIngestionService.getCompletedFileEntityById(fileId);

        TopicFileEntity entity = TopicFileEntity.builder()
                .topic(topic)
                .dataIngestion(dataIngestion)
                .summary(summary)
                .metadata(metadata)
                .build();

        return topicFileMapper.entityToResponseDto(topicFileRepository.save(entity));
    }

    @Transactional
    public TopicFileResponseDto updateMessageId(UUID topicFileId, UUID messageId) {
        TopicFileEntity entity = topicFileRepository.findById(topicFileId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.TOPIC_FILE_NOT_EXISTS));

        topicService.validateTopicOfUser(entity.getTopic().getId(), JwtUtil.getUserId());
        entity.setMessageId(messageId);

        return topicFileMapper.entityToResponseDto(topicFileRepository.save(entity));
    }

    /**
     * Xóa một file đã được đính kèm khỏi topic. Phương thức này sẽ kiểm tra quyền truy cập của người dùng trước khi thực hiện xóa.
     * @param topicId ID của topic
     * @param fileId ID của file
     */
    @Transactional
    public void removeFile(UUID topicId, UUID fileId) {
        topicService.validateTopicOfUser(topicId, JwtUtil.getUserId());

        TopicFileEntity entity = topicFileRepository.findByTopicIdAndDataIngestionId(topicId, fileId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.TOPIC_FILE_NOT_EXISTS));

        topicFileRepository.delete(entity);
    }

    /**
     * Tải lên một file và đính kèm vào topic. Phương thức này sẽ chờ đến khi quá trình ingestion hoàn tất mới trả về kết quả.
     * @param topicId ID của topic
     * @param file File cần tải lên
     * @param userId ID của người dùng
     * @param orgId ID của tổ chức
     * @return Thông tin về file đã được đính kèm vào topic
     */
    private TopicFileResponseDto uploadSingleFileAndAttach(UUID topicId, MultipartFile file, UUID userId, UUID orgId) {
        DataIngestionUploadRequestDto requestDto = new DataIngestionUploadRequestDto();
        requestDto.setFile(file);
        requestDto.setAccessLevel(DataScope.PERSONAL);

        UUID dataIngestionId = dataIngestionService.uploadDataIngestion(requestDto, userId, orgId, DataSource.TOPIC).getId();
        dataIngestionService.waitForIngestionCompleted(dataIngestionId);
        return addFile(topicId, dataIngestionId, null, null, userId);
    }

    /**
     * Giải nén CompletionException để lấy ra RuntimeException gốc. Phương thức này sẽ lặp qua các nguyên nhân của CompletionException cho đến khi tìm thấy một RuntimeException hoặc không còn nguyên nhân nào nữa.
     * @param exception CompletionException cần giải nén
     * @return RuntimeException gốc nếu tìm thấy, hoặc một AppException với mã lỗi UNEXPECTED nếu không tìm thấy RuntimeException nào
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
}
