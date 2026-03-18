package ai.service;

import ai.dto.outer.ingestion.response.IngestionStatusResponseDto;
import ai.dto.outer.ingestion.response.IngestionUploadResponseDto;
import ai.dto.own.request.MediaCreateFolderRequestDto;
import ai.dto.own.request.MediaRetryIngestionRequestDto;
import ai.dto.own.request.MediaUpdateFolderRequestDto;
import ai.dto.own.request.MediaUploadRequestDto;
import ai.dto.own.response.MediaJobStatusResponseDto;
import ai.dto.own.response.MediaResponseDto;
import ai.dto.own.response.MediaUploadResponseDto;
import ai.entity.postgres.MediaEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.MediaUploadTarget;
import ai.exeption.AppException;
import ai.repository.MediaRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class MediaService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "type",
            "size",
            "createdAt",
            "updatedAt",
            "downloadCount",
            "ingestionStatus",
            "accessLevel"
    );

    MediaRepository mediaRepository;
    IngestionService ingestionService;
    MinioService minioService;

    @Transactional(noRollbackFor = AppException.class)
    public MediaUploadResponseDto uploadMedia(MediaUploadRequestDto requestDto) {
        validateUploadRequest(requestDto);

        String unitValue = resolveUnitValue(requestDto);
        String visibilityValue = resolveVisibilityValue(requestDto.getVisibility());

        String minioPath = minioService.upload(requestDto.getFile(), unitValue);

        MediaEntity media = new MediaEntity();
        media.setName(requestDto.getFile().getOriginalFilename());
        media.setMinioPath(minioPath);
        media.setSize(requestDto.getFile().getSize());
        media.setType(resolveFileType(requestDto.getFile().getOriginalFilename()));
        media.setAccessLevel(visibilityValue);
        media.setOwnerId(requestDto.getOwnerId());
        media.setOrgId(requestDto.getOrgId());
        media.setTarget(requestDto.getTarget());
        media.setIngestionStatus(isIngestionTarget(requestDto.getTarget()) ? "PENDING" : "NONE");

        if (requestDto.getParentId() != null) {
            MediaEntity parent = mediaRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_PARENT_NOT_EXISTS));
            media.setParent(parent);
        }

        media = mediaRepository.save(media);

        if (requestDto.getTarget() == MediaUploadTarget.AVATAR) {
            return MediaUploadResponseDto.builder()
                    .mediaId(media.getId())
                    .minioPath(media.getMinioPath())
                    .target(media.getTarget())
                    .ingestionStatus(media.getIngestionStatus())
                    .build();
        }

        try {
            IngestionUploadResponseDto ingestionResponse = ingestionService.pushToVector(
                    requestDto.getFile(),
                    requestDto.getUsername(),
                    unitValue,
                    visibilityValue
            );

            if (ingestionResponse == null || ingestionResponse.getJobId() == null) {
                media.setIngestionStatus("FAILED");
                mediaRepository.save(media);
                throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
            }

            media.setJobId(ingestionResponse.getJobId());
            media.setIngestionStatus("PENDING");
            mediaRepository.save(media);

            return MediaUploadResponseDto.builder()
                    .mediaId(media.getId())
                    .minioPath(media.getMinioPath())
                    .target(media.getTarget())
                    .ingestionStatus(media.getIngestionStatus())
                    .jobId(media.getJobId())
                    .build();
        } catch (AppException exception) {
            media.setIngestionStatus("FAILED");
            mediaRepository.save(media);
            throw exception;
        }
    }

    @Transactional
    public MediaResponseDto createFolder(MediaCreateFolderRequestDto requestDto) {
        validateCreateFolderRequest(requestDto);

        MediaEntity media = new MediaEntity();
        media.setName(requestDto.getName().trim());
        media.setType("FOLDER");
        media.setSize(0L);
        media.setAccessLevel(resolveVisibilityValue(requestDto.getVisibility()));
        media.setOwnerId(requestDto.getOwnerId());
        media.setOrgId(requestDto.getOrgId());
        media.setTarget(requestDto.getTarget());
        media.setIngestionStatus("NONE");

        if (requestDto.getParentId() != null) {
            MediaEntity parent = mediaRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_PARENT_NOT_EXISTS));
            media.setParent(parent);
        }

        return toResponseDto(mediaRepository.save(media));
    }

    @Transactional(readOnly = true)
    public MediaResponseDto getById(UUID mediaId) {
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_NOT_EXISTS));
        return toResponseDto(media);
    }

    @Transactional
    public MediaResponseDto updateFolder(UUID mediaId, MediaUpdateFolderRequestDto requestDto) {
        MediaEntity folder = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_NOT_EXISTS));

        if (!"FOLDER".equalsIgnoreCase(folder.getType())) {
            throw new AppException(ApiResponseStatus.MEDIA_FOLDER_ONLY_OPERATION);
        }

        boolean hasName = requestDto != null && requestDto.getName() != null && !requestDto.getName().isBlank();
        boolean hasParent = requestDto != null && requestDto.getParentId() != null;
        boolean moveToRoot = requestDto != null && Boolean.TRUE.equals(requestDto.getMoveToRoot());
        if (!hasName && !hasParent && !moveToRoot) {
            throw new AppException(ApiResponseStatus.MEDIA_FOLDER_UPDATE_REQUIRED);
        }

        if (hasName) {
            folder.setName(requestDto.getName().trim());
        }

        if (moveToRoot) {
            folder.setParent(null);
        } else if (hasParent) {
            MediaEntity parent = mediaRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_PARENT_NOT_EXISTS));

            if (!"FOLDER".equalsIgnoreCase(parent.getType())) {
                throw new AppException(ApiResponseStatus.MEDIA_PARENT_MUST_BE_FOLDER);
            }
            if (folder.getId().equals(parent.getId())) {
                throw new AppException(ApiResponseStatus.MEDIA_MOVE_CYCLE_NOT_ALLOWED);
            }
            if (isDescendantOf(parent, folder)) {
                throw new AppException(ApiResponseStatus.MEDIA_MOVE_CYCLE_NOT_ALLOWED);
            }

            folder.setParent(parent);
        }

        return toResponseDto(mediaRepository.save(folder));
    }

    @Transactional(readOnly = true)
    public Page<MediaResponseDto> listMedia(UUID orgId, UUID ownerId, UUID parentId, MediaUploadTarget target,
                                            Integer pageNumber, Integer pageSize, String sortBy, String sortDir) {
        if (orgId == null) {
            throw new AppException(ApiResponseStatus.MEDIA_ORG_ID_REQUIRED);
        }

        Pageable pageable = createPageable(pageNumber, pageSize, sortBy, sortDir);

        return mediaRepository.findByOrgAndOptionalOwnerAndParent(orgId, ownerId, parentId, target, pageable)
                .map(this::toResponseDto);
    }

    @Transactional(noRollbackFor = AppException.class)
    public MediaUploadResponseDto retryIngestion(UUID mediaId, MediaRetryIngestionRequestDto requestDto) {
        if (requestDto == null || requestDto.getUsername() == null || requestDto.getUsername().isBlank()) {
            throw new AppException(ApiResponseStatus.MEDIA_USERNAME_REQUIRED);
        }
        if (requestDto.getUnit() == null || requestDto.getUnit().isBlank()) {
            throw new AppException(ApiResponseStatus.MEDIA_UNIT_REQUIRED);
        }

        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_NOT_EXISTS));

        if (!isIngestionTarget(media.getTarget())) {
            throw new AppException(ApiResponseStatus.MEDIA_INGESTION_RETRY_ONLY_INGESTION_TARGET);
        }
        if (!"FAILED".equalsIgnoreCase(media.getIngestionStatus())) {
            throw new AppException(ApiResponseStatus.MEDIA_INGESTION_RETRY_ONLY_FAILED);
        }
        if (media.getMinioPath() == null || media.getMinioPath().isBlank()) {
            throw new AppException(ApiResponseStatus.MEDIA_UPLOAD_FAILED);
        }

        String visibilityValue = requestDto.getVisibility() != null && !requestDto.getVisibility().isBlank()
                ? requestDto.getVisibility()
                : media.getAccessLevel();

        try {
            MinioService.MinioObjectData objectData = minioService.download(media.getMinioPath());
            IngestionUploadResponseDto ingestionResponse = ingestionService.pushToVector(
                    objectData.getBytes(),
                    media.getName(),
                    requestDto.getUsername(),
                    requestDto.getUnit(),
                    resolveVisibilityValue(visibilityValue)
            );

            if (ingestionResponse == null || ingestionResponse.getJobId() == null) {
                media.setIngestionStatus("FAILED");
                mediaRepository.save(media);
                throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
            }

            media.setJobId(ingestionResponse.getJobId());
            media.setIngestionStatus("PENDING");
            mediaRepository.save(media);

            return MediaUploadResponseDto.builder()
                    .mediaId(media.getId())
                    .minioPath(media.getMinioPath())
                    .target(media.getTarget())
                    .ingestionStatus(media.getIngestionStatus())
                    .jobId(media.getJobId())
                    .build();
        } catch (AppException exception) {
            media.setIngestionStatus("FAILED");
            mediaRepository.save(media);
            throw exception;
        }
    }

    @Transactional
    public MediaJobStatusResponseDto pollIngestionJobStatus(UUID jobId) {
        IngestionStatusResponseDto ingestionStatusResponse = ingestionService.getJobStatus(jobId);

        String resolvedStatus = resolveStatus(ingestionStatusResponse);
        Optional<MediaEntity> mediaOptional = mediaRepository.findByJobId(jobId);
        if (mediaOptional.isPresent()) {
            MediaEntity media = mediaOptional.get();
            media.setIngestionStatus(resolvedStatus);
            mediaRepository.save(media);

            return MediaJobStatusResponseDto.builder()
                    .mediaId(media.getId())
                    .jobId(jobId)
                    .ingestionStatus(resolvedStatus)
                    .message(ingestionStatusResponse.getMessage())
                    .build();
        }

        if (ingestionStatusResponse.getJobId() == null) {
            throw new AppException(ApiResponseStatus.MEDIA_JOB_ID_NOT_EXISTS);
        }

        return MediaJobStatusResponseDto.builder()
                .jobId(jobId)
                .ingestionStatus(resolvedStatus)
                .message(ingestionStatusResponse.getMessage())
                .build();
    }

    private String resolveStatus(IngestionStatusResponseDto responseDto) {
        if (responseDto == null || responseDto.getStatus() == null || responseDto.getStatus().isBlank()) {
            return "PENDING";
        }
        return responseDto.getStatus().trim().toUpperCase(Locale.ROOT);
    }

    private MediaResponseDto toResponseDto(MediaEntity media) {
        return MediaResponseDto.builder()
                .id(media.getId())
                .name(media.getName())
                .type(media.getType())
                .size(media.getSize())
                .minioPath(media.getMinioPath())
                .parentId(media.getParent() != null ? media.getParent().getId() : null)
                .ownerId(media.getOwnerId())
                .orgId(media.getOrgId())
                .accessLevel(media.getAccessLevel())
                .jobId(media.getJobId())
                .ingestionStatus(media.getIngestionStatus())
                .downloadCount(media.getDownloadCount())
                .createdAt(media.getAudit() != null ? media.getAudit().getCreatedAt() : null)
                .updatedAt(media.getAudit() != null ? media.getAudit().getUpdatedAt() : null)
                .target(media.getTarget())
                .build();
    }

    private void validateUploadRequest(MediaUploadRequestDto requestDto) {
        if (requestDto.getFile() == null || requestDto.getFile().isEmpty()) {
            throw new AppException(ApiResponseStatus.MEDIA_FILE_REQUIRED);
        }
        if (requestDto.getTarget() == null) {
            throw new AppException(ApiResponseStatus.MEDIA_TARGET_INVALID);
        }
        if (requestDto.getOrgId() == null) {
            throw new AppException(ApiResponseStatus.MEDIA_ORG_ID_REQUIRED);
        }
        if (requestDto.getOwnerId() == null) {
            throw new AppException(ApiResponseStatus.MEDIA_OWNER_ID_REQUIRED);
        }
        if (isIngestionTarget(requestDto.getTarget())) {
            if (requestDto.getUnit() == null || requestDto.getUnit().isBlank()) {
                throw new AppException(ApiResponseStatus.MEDIA_UNIT_REQUIRED);
            }
            if (requestDto.getUsername() == null || requestDto.getUsername().isBlank()) {
                throw new AppException(ApiResponseStatus.MEDIA_USERNAME_REQUIRED);
            }
        }
    }

    private void validateCreateFolderRequest(MediaCreateFolderRequestDto requestDto) {
        if (requestDto == null || requestDto.getName() == null || requestDto.getName().isBlank()) {
            throw new AppException(ApiResponseStatus.MEDIA_NAME_REQUIRED);
        }
        if (requestDto.getTarget() == null) {
            throw new AppException(ApiResponseStatus.MEDIA_TARGET_INVALID);
        }
        if (requestDto.getOrgId() == null) {
            throw new AppException(ApiResponseStatus.MEDIA_ORG_ID_REQUIRED);
        }
        if (requestDto.getOwnerId() == null) {
            throw new AppException(ApiResponseStatus.MEDIA_OWNER_ID_REQUIRED);
        }
    }

    private Pageable createPageable(Integer pageNumber, Integer pageSize, String sortBy, String sortDir) {
        int resolvedPage = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        int resolvedSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;

        String normalizedSortDir = sortDir == null ? "ASC" : sortDir.trim().toUpperCase(Locale.ROOT);
        if (!"ASC".equals(normalizedSortDir) && !"DESC".equals(normalizedSortDir)) {
            throw new AppException(ApiResponseStatus.MEDIA_SORT_DIR_INVALID);
        }

        if (sortBy == null || sortBy.isBlank()) {
            return PageRequest.of(resolvedPage, resolvedSize);
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new AppException(ApiResponseStatus.MEDIA_SORT_BY_INVALID);
        }

        Sort.Direction direction = "DESC".equals(normalizedSortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, mapSortField(sortBy)));
    }

    private String mapSortField(String sortBy) {
        if ("createdAt".equals(sortBy)) {
            return "audit.createdAt";
        }
        if ("updatedAt".equals(sortBy)) {
            return "audit.updatedAt";
        }
        return sortBy;
    }

    private boolean isDescendantOf(MediaEntity candidateParent, MediaEntity node) {
        MediaEntity cursor = candidateParent;
        while (cursor != null) {
            if (cursor.getId().equals(node.getId())) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    private boolean isIngestionTarget(MediaUploadTarget target) {
        return target == MediaUploadTarget.INGESTION || target == MediaUploadTarget.RAG;
    }

    private String resolveUnitValue(MediaUploadRequestDto requestDto) {
        if (requestDto.getUnit() != null && !requestDto.getUnit().isBlank()) {
            return requestDto.getUnit().trim();
        }
        return requestDto.getTarget() == MediaUploadTarget.AVATAR ? "avatar" : "documents";
    }

    private String resolveVisibilityValue(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return "private";
        }
        return visibility.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveFileType(String filename) {
        if (filename == null || filename.isBlank()) {
            return "bin";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == filename.length() - 1) {
            return "bin";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
