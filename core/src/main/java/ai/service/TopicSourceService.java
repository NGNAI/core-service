package ai.service;

import ai.dto.own.request.TopicSourcesAddRequestDto;
import ai.dto.own.response.TopicSourceDownloadData;
import ai.dto.own.response.TopicSourcePresignedUrlResponseDto;
import ai.dto.own.response.TopicSourceResponseDto;
import ai.entity.postgres.TopicEntity;
import ai.entity.postgres.TopicSourceEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.TopicSourceMapper;
import ai.repository.TopicSourceRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
public class TopicSourceService {
    static final String TOPIC_BUCKET = "knowledgetopics";
    static final int DEFAULT_PRESIGNED_EXPIRY_SECONDS = 900;

    TopicSourceRepository topicSourceRepository;
    TopicSourceMapper topicSourceMapper;
    TopicService topicService;
    MinioService minioService;

    
    public Pair<Long, List<TopicSourceResponseDto>> getSources(UUID topicId, int page, int size) {
        topicService.validateTopicOfUser(topicId, JwtUtil.getUserId());
        Page<TopicSourceEntity> result = topicSourceRepository.findByTopicId(topicId, PageRequest.of(page, size));
        return Pair.of(result.getTotalElements(),
                result.getContent().stream().map(topicSourceMapper::entityToResponseDto).toList());
    }

    public List<TopicSourceResponseDto> getAllSources(UUID topicId) {
        topicService.validateTopicOfUser(topicId, JwtUtil.getUserId());
        List<TopicSourceEntity> result = topicSourceRepository.findByTopicId(topicId);
        return result.stream().map(topicSourceMapper::entityToResponseDto).toList();
    }   
    
    public List<TopicSourceResponseDto> uploadSources(UUID topicId, TopicSourcesAddRequestDto requestDto) {
        UUID userId = JwtUtil.getUserId();
        topicService.validateTopicOfUser(topicId, userId);

        MultipartFile[] files = requestDto == null ? null : requestDto.getFiles();
        List<MultipartFile> validFiles = Arrays.stream(files == null ? new MultipartFile[0] : files)
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (validFiles.isEmpty()) {
            throw new AppException(ApiResponseStatus.TOPIC_SOURCE_PAYLOAD_REQUIRED);
        }

        int poolSize = Math.min(validFiles.size(), Math.max(1, Runtime.getRuntime().availableProcessors()));
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

        try {
            List<CompletableFuture<TopicSourceResponseDto>> futures = validFiles.stream()
                    .map(file -> CompletableFuture.supplyAsync(
                            () -> uploadSingleFileAndAttach(topicId, file, userId),
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

    @Transactional
    public void removeSource(UUID topicId, UUID sourceId) {
        topicService.validateTopicOfUser(topicId, JwtUtil.getUserId());
        TopicSourceEntity entity = getSourceEntity(topicId, sourceId);
        topicSourceRepository.delete(entity);
    }

    public TopicSourceDownloadData downloadSource(UUID topicId, UUID sourceId) {
        topicService.validateTopicOfUser(topicId, JwtUtil.getUserId());
        TopicSourceEntity source = getSourceEntity(topicId, sourceId);
        validateDownloadableSource(source);

        MinioService.MinioObjectData objectData = minioService.download(source.getFilePath(), TOPIC_BUCKET);
        return new TopicSourceDownloadData(resolveFileName(source), objectData.getContentType(), objectData.getBytes());
    }

    public TopicSourcePresignedUrlResponseDto getSourceDownloadUrl(UUID topicId, UUID sourceId, Integer expiresInSeconds) {
        topicService.validateTopicOfUser(topicId, JwtUtil.getUserId());
        TopicSourceEntity source = getSourceEntity(topicId, sourceId);
        validateDownloadableSource(source);

        int effectiveExpiry = expiresInSeconds == null || expiresInSeconds <= 0
                ? DEFAULT_PRESIGNED_EXPIRY_SECONDS
                : expiresInSeconds;

        String url = minioService.generatePresignedDownloadUrl(source.getFilePath(), effectiveExpiry, TOPIC_BUCKET);
        return TopicSourcePresignedUrlResponseDto.builder()
                .url(url)
                .expiresInSeconds(effectiveExpiry)
                .build();
    }

    private TopicSourceResponseDto uploadSingleFileAndAttach(UUID topicId, MultipartFile file, UUID userId) {
        String originalName = file.getOriginalFilename();
        String displayName = (originalName == null || originalName.isBlank())
                ? "unnamed-source"
                : originalName;

        if (topicSourceRepository.existsByTopicIdAndDisplayNameAndSourceType(
                topicId,
                displayName,
                TopicSourceEntity.SourceType.FILE)) {
            throw new AppException(ApiResponseStatus.TOPIC_SOURCE_ALREADY_EXISTS);
        }

        String objectPath = minioService.upload(
                file,
                userId.toString(),
                topicId.toString(),
                TOPIC_BUCKET);

        TopicEntity topic = topicService.getEntityById(topicId);
        TopicSourceEntity entity = TopicSourceEntity.builder()
                .topic(topic)
                .sourceType(TopicSourceEntity.SourceType.FILE)
                .displayName(displayName)
                .rawContent(null)
                .filePath(objectPath)
                .summary(null)
                .metadata(null)
                .vectorStatus(TopicSourceEntity.VectorStatus.NOT_PROCESSED)
                .build();

        return topicSourceMapper.entityToResponseDto(topicSourceRepository.save(entity));
    }

    private TopicSourceEntity getSourceEntity(UUID topicId, UUID sourceId) {
        return topicSourceRepository.findByTopicIdAndId(topicId, sourceId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.TOPIC_SOURCE_NOT_EXISTS));
    }

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

    private void validateDownloadableSource(TopicSourceEntity source) {
        if (!TopicSourceEntity.SourceType.FILE.equals(source.getSourceType())
                || source.getFilePath() == null
                || source.getFilePath().isBlank()) {
            throw new AppException(ApiResponseStatus.TOPIC_SOURCE_NOT_EXISTS);
        }
    }

    private String resolveFileName(TopicSourceEntity source) {
        if (source.getDisplayName() != null && !source.getDisplayName().isBlank()) {
            return source.getDisplayName();
        }

        Path filePath = Path.of(source.getFilePath());
        Path fileName = filePath.getFileName();
        return fileName == null ? source.getId().toString() : fileName.toString();
    }
}
