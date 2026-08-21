package ai.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

import ai.dto.outer.ingestion.response.IngestionStatusResponseDto;
import ai.dto.outer.ingestion.response.IngestionUploadResponseDto;
import ai.dto.own.request.DraftSourcesAddRequestDto;
import ai.dto.own.response.DraftSourceDownloadData;
import ai.dto.own.response.DraftSourcePresignedUrlResponseDto;
import ai.dto.own.response.DraftSourceResponseDto;
import ai.entity.postgres.DraftEntity;
import ai.entity.postgres.DraftSourceEntity;
import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.DataScope;
import ai.exception.AppException;
import ai.mapper.DraftSourceMapper;
import ai.repository.DraftRepository;
import ai.repository.DraftSourceRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class DraftSourceService {
    static final String DRAFT_BUCKET = "knowledgedrafts";
    static final int DEFAULT_PRESIGNED_EXPIRY_SECONDS = 900;
    static final long DEFAULT_INGESTION_WAIT_TIMEOUT_MILLIS = 180_000;
    static final long DEFAULT_INGESTION_POLL_INTERVAL_MILLIS = 2_000;

    DraftSourceRepository draftSourceRepository;
    DraftSourceMapper draftSourceMapper;
    DraftRepository draftRepository;
    MinioService minioService;
    IngestionService ingestionService;
    UserService userService;
    OrganizationService organizationService;

    /**
     * Lấy sources cho user flow — <b>có kiểm tra ownership</b>.
     */
    public Pair<Long, List<DraftSourceResponseDto>> getSources(UUID draftId, int page, int size) {
        validateDraftOfUser(draftId, JwtUtil.getUserId());
        return getSourcesShared(draftId, page, size);
    }

    /**
     * Lấy tất cả sources (không phân trang) cho user flow — <b>có kiểm tra ownership</b>.
     */
    public List<DraftSourceResponseDto> getAllSources(UUID draftId) {
        validateDraftOfUser(draftId, JwtUtil.getUserId());
        List<DraftSourceEntity> result = draftSourceRepository.findByDraftId(draftId);
        return result.stream().map(draftSourceMapper::entityToResponseDto).toList();
    }

    /**
     * Lấy sources cho public share link flow — <b>không kiểm tra ownership</b>.
     */
    public Pair<Long, List<DraftSourceResponseDto>> getSourcesShared(UUID draftId, int page, int size) {
        Page<DraftSourceEntity> result = draftSourceRepository.findByDraftId(draftId, PageRequest.of(page, size));
        return Pair.of(result.getTotalElements(),
                result.getContent().stream().map(draftSourceMapper::entityToResponseDto).toList());
    }

    /**
     * Lấy tất cả sources (không phân trang) cho public share link flow — <b>không kiểm tra ownership</b>.
     */
    public List<DraftSourceResponseDto> getAllSourcesShared(UUID draftId) {
        List<DraftSourceEntity> result = draftSourceRepository.findByDraftId(draftId);
        return result.stream().map(draftSourceMapper::entityToResponseDto).toList();
    }

    /**
     * Upload một hoặc nhiều file lên MinIO và tạo DraftSourceEntity. Không chờ embedding.
     */
    public List<DraftSourceResponseDto> uploadSources(UUID draftId, DraftSourcesAddRequestDto requestDto) {
        UUID userId = JwtUtil.getUserId();
        validateDraftOfUser(draftId, userId);

        MultipartFile[] files = requestDto == null ? null : requestDto.getFiles();
        List<MultipartFile> validFiles = Arrays.stream(files == null ? new MultipartFile[0] : files)
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (validFiles.isEmpty()) {
            throw new AppException(ApiResponseStatus.DRAFT_SOURCE_PAYLOAD_REQUIRED);
        }

        int poolSize = Math.min(validFiles.size(), Math.max(1, Runtime.getRuntime().availableProcessors()));
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

        try {
            List<CompletableFuture<DraftSourceResponseDto>> futures = validFiles.stream()
                    .map(file -> CompletableFuture.supplyAsync(
                            () -> uploadSingleFileAndAttach(draftId, file, userId),
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
     * Đảm bảo mọi source FILE của draft đã ingestion (vector) hoàn tất trước khi chat.
     * Source chưa COMPLETED sẽ được gửi lên ingestion service và poll tới khi COMPLETED/FAILED/timeout.
     * @return set source id đã sẵn sàng dùng làm file_ids cho RAG
     */
    public Set<String> ingestAndWaitAllSourcesReady(UUID draftId) {
        UserEntity user = userService.getEntityById(JwtUtil.getUserId());
        OrganizationEntity organization = organizationService.getEntityById(JwtUtil.getOrgId());

        Set<String> readyFileIds = new HashSet<>();
        for (DraftSourceEntity source : draftSourceRepository.findByDraftId(draftId)) {
            if (source == null || !DraftSourceEntity.SourceType.FILE.equals(source.getSourceType())) {
                continue;
            }

            if (!DraftSourceEntity.VectorStatus.COMPLETED.equals(source.getVectorStatus())) {
                ingestChatSourceAndWaitUntilReady(draftId, source, user, organization);
            }

            readyFileIds.add(source.getId().toString());
        }
        return readyFileIds;
    }

    @Transactional
    public void removeSource(UUID draftId, UUID sourceId) {
        validateDraftOfUser(draftId, JwtUtil.getUserId());
        DraftSourceEntity entity = getSourceEntity(draftId, sourceId);
        draftSourceRepository.delete(entity);
    }

    public DraftSourceDownloadData downloadSource(UUID draftId, UUID sourceId) {
        validateDraftOfUser(draftId, JwtUtil.getUserId());
        DraftSourceEntity source = getSourceEntity(draftId, sourceId);
        validateDownloadableSource(source);

        MinioService.MinioObjectStream objectStream = minioService.getObjectStream(source.getFilePath(), DRAFT_BUCKET);
        return new DraftSourceDownloadData(resolveFileName(source), objectStream.getContentType(), objectStream.getInputStream(), objectStream.getSize());
    }

    public DraftSourcePresignedUrlResponseDto getSourceDownloadUrl(UUID draftId, UUID sourceId, Integer expiresInSeconds) {
        validateDraftOfUser(draftId, JwtUtil.getUserId());
        return getSourceDownloadUrlShared(draftId, sourceId, expiresInSeconds);
    }

    /**
     * Lấy presigned download URL cho public share link flow — <b>không kiểm tra ownership</b>.
     */
    public DraftSourcePresignedUrlResponseDto getSourceDownloadUrlShared(UUID draftId, UUID sourceId, Integer expiresInSeconds) {
        DraftSourceEntity source = getSourceEntity(draftId, sourceId);
        validateDownloadableSource(source);

        int effectiveExpiry = expiresInSeconds == null || expiresInSeconds <= 0
                ? DEFAULT_PRESIGNED_EXPIRY_SECONDS
                : expiresInSeconds;

        String url = minioService.generatePresignedDownloadUrl(source.getFilePath(), effectiveExpiry, DRAFT_BUCKET);
        return DraftSourcePresignedUrlResponseDto.builder()
                .url(url)
                .expiresInSeconds(effectiveExpiry)
                .build();
    }

    /**
     * Upload single file and create DraftSourceEntity.
     */
    private DraftSourceResponseDto uploadSingleFileAndAttach(UUID draftId, MultipartFile file, UUID userId) {
        String originalName = file.getOriginalFilename();
        String displayName = (originalName == null || originalName.isBlank())
                ? "unnamed-source"
                : originalName;

        if (draftSourceRepository.existsByDraftIdAndDisplayNameAndSourceType(
                draftId,
                displayName,
                DraftSourceEntity.SourceType.FILE)) {
            throw new AppException(ApiResponseStatus.DRAFT_SOURCE_ALREADY_EXISTS);
        }

        String objectPath = minioService.upload(
                file,
                userId.toString(),
                draftId.toString(),
                DRAFT_BUCKET);

        DraftEntity draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DRAFT_ID_NOT_EXISTS));
        DraftSourceEntity entity = DraftSourceEntity.builder()
                .draft(draft)
                .sourceType(DraftSourceEntity.SourceType.FILE)
                .displayName(displayName)
                .rawContent(null)
                .filePath(objectPath)
                .summary(null)
                .metadata(null)
                .vectorStatus(DraftSourceEntity.VectorStatus.CREATED)
                .build();

        return draftSourceMapper.entityToResponseDto(draftSourceRepository.save(entity));
    }

    /**
     * Gửi source lên ingestion service để xử lý embedding và chờ tới khi hoàn tất.
     * Poll trạng thái job định kỳ, cập nhật vectorStatus. Nếu lỗi hoặc timeout sẽ ném DATA_INGESTION_NOT_COMPLETED.
     */
    private void ingestChatSourceAndWaitUntilReady(
            UUID draftId,
            DraftSourceEntity source,
            UserEntity user,
            OrganizationEntity organization) {
        if (source == null
                || !DraftSourceEntity.SourceType.FILE.equals(source.getSourceType())
                || source.getFilePath() == null
                || source.getFilePath().isBlank()) {
            throw new AppException(ApiResponseStatus.DRAFT_SOURCE_NOT_EXISTS);
        }

        source.setVectorStatus(DraftSourceEntity.VectorStatus.CREATED);
        source = draftSourceRepository.save(source);

        IngestionUploadResponseDto ingestionResponse;
        // Stream trực tiếp từ MinIO lên ingestion service để tránh load toàn bộ file vào RAM
        try (MinioService.MinioObjectStream objectStream = minioService.getObjectStream(source.getFilePath(), DRAFT_BUCKET)) {
            ingestionResponse = ingestionService.uploadChat(
                    objectStream.getInputStream(),
                    objectStream.getSize(),
                    resolveFileName(source),
                    source.getId().toString(),
                    user.getId().toString(),
                    user.getUserName(),
                    organization.getId().toString(),
                    organization.getName(),
                    DataScope.PERSONAL,
                    draftId.toString(),
                    null);
        }

        if (ingestionResponse == null || ingestionResponse.getJobId() == null) {
            source.setVectorStatus(DraftSourceEntity.VectorStatus.FAILED);
            draftSourceRepository.save(source);
            throw new AppException(ApiResponseStatus.DATA_INGESTION_NOT_COMPLETED);
        }

        long deadline = System.currentTimeMillis() + DEFAULT_INGESTION_WAIT_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() <= deadline) {
            IngestionStatusResponseDto statusResponse = ingestionService.getJobStatus(ingestionResponse.getJobId());
            DraftSourceEntity.VectorStatus resolvedStatus = resolveVectorStatus(
                    statusResponse == null ? null : statusResponse.getStatus(),
                    source.getVectorStatus());

            if (!resolvedStatus.equals(source.getVectorStatus())) {
                source.setVectorStatus(resolvedStatus);
                source = draftSourceRepository.save(source);
            }

            if (DraftSourceEntity.VectorStatus.COMPLETED.equals(resolvedStatus)) {
                return;
            }

            if (DraftSourceEntity.VectorStatus.FAILED.equals(resolvedStatus)) {
                throw new AppException(ApiResponseStatus.DATA_INGESTION_NOT_COMPLETED);
            }

            try {
                Thread.sleep(DEFAULT_INGESTION_POLL_INTERVAL_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AppException(ApiResponseStatus.DATA_INGESTION_NOT_COMPLETED);
            }
        }

        throw new AppException(ApiResponseStatus.DATA_INGESTION_NOT_COMPLETED);
    }

    /**
     * Chuyển trạng thái thô từ ingestion service sang VectorStatus tương ứng.
     */
    private DraftSourceEntity.VectorStatus resolveVectorStatus(
            String rawStatus,
            DraftSourceEntity.VectorStatus fallbackStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return fallbackStatus == null ? DraftSourceEntity.VectorStatus.CREATED : fallbackStatus;
        }

        String normalized = rawStatus.trim().toUpperCase();
        if ("SUCCESS".equals(normalized)
                || "DONE".equals(normalized)
                || "COMPLETED".equals(normalized)) {
            return DraftSourceEntity.VectorStatus.COMPLETED;
        }

        if ("ERROR".equals(normalized) || "FAILED".equals(normalized)) {
            return DraftSourceEntity.VectorStatus.FAILED;
        }

        if ("EXTRACTING".equals(normalized)) {
            return DraftSourceEntity.VectorStatus.EXTRACTING;
        }

        if ("CHUNKING".equals(normalized)) {
            return DraftSourceEntity.VectorStatus.CHUNKING;
        }

        if ("EMBEDDING".equals(normalized)) {
            return DraftSourceEntity.VectorStatus.EMBEDDING;
        }

        if ("STORING".equals(normalized)) {
            return DraftSourceEntity.VectorStatus.STORING;
        }

        return DraftSourceEntity.VectorStatus.CREATED;
    }

    private DraftSourceEntity getSourceEntity(UUID draftId, UUID sourceId) {
        return draftSourceRepository.findByDraftIdAndId(draftId, sourceId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DRAFT_SOURCE_NOT_EXISTS));
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

    private void validateDraftOfUser(UUID draftId, UUID userId) {
        if (!draftRepository.existsByIdAndOwnerId(draftId, userId)) {
            throw new AppException(ApiResponseStatus.PERMISSION_DENIED);
        }
    }

    private void validateDownloadableSource(DraftSourceEntity source) {
        if (!DraftSourceEntity.SourceType.FILE.equals(source.getSourceType())
                || source.getFilePath() == null
                || source.getFilePath().isBlank()) {
            throw new AppException(ApiResponseStatus.DRAFT_SOURCE_NOT_EXISTS);
        }
    }

    private String resolveFileName(DraftSourceEntity source) {
        if (source.getDisplayName() != null && !source.getDisplayName().isBlank()) {
            return source.getDisplayName();
        }

        java.nio.file.Path filePath = java.nio.file.Path.of(source.getFilePath());
        java.nio.file.Path fileName = filePath.getFileName();
        return fileName == null ? source.getId().toString() : fileName.toString();
    }
}
