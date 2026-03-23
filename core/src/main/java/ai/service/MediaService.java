package ai.service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.dto.outer.ingestion.response.IngestionStatusResponseDto;
import ai.dto.outer.ingestion.response.IngestionUploadResponseDto;
import ai.dto.own.request.MediaCreateFolderRequestDto;
import ai.dto.own.request.MediaUpdateFolderRequestDto;
import ai.dto.own.request.MediaUploadRequestDto;
import ai.dto.own.request.filter.MediaFilterDto;
import ai.dto.own.response.MediaDownloadData;
import ai.dto.own.response.MediaJobStatusResponseDto;
import ai.dto.own.response.MediaPresignedUrlResponseDto;
import ai.dto.own.response.MediaResponseDto;
import ai.entity.postgres.MediaEntity;
import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.IngestionStatus;
import ai.enums.MediaUploadTarget;
import ai.exeption.AppException;
import ai.mapper.MediaMapper;
import ai.repository.MediaRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class MediaService {
    private static final int DEFAULT_PRESIGNED_EXPIRY_SECONDS = 900;

    MediaRepository mediaRepository;
    IngestionService ingestionService;
    MinioService minioService;
    MediaMapper mediaMapper;
    UserService userService;
    OrganizationService organizationService;

    @Transactional(noRollbackFor = AppException.class)
    public MediaResponseDto uploadMedia(MediaUploadRequestDto requestDto) {
        UserEntity user = userService.getEntityById(JwtUtil.getUserId());
        OrganizationEntity organization = organizationService.getEntityById(JwtUtil.getOrgId());

        // Push lên MinIO trước để tránh trường hợp đã lưu media vào database nhưng
        // upload file lên MinIO thất bại, dẫn đến media bị lỗi không thể retry
        // ingestion được
        String minioPath = minioService.upload(requestDto.getFile(), user.getUserName(), organization.getName());

        MediaEntity media = new MediaEntity();
        media.setName(requestDto.getFile().getOriginalFilename());
        media.setFolder(false);
        media.setMinioPath(minioPath);
        media.setFileSize(requestDto.getFile().getSize());
        media.setContentType(resolveFileType(requestDto.getFile().getOriginalFilename()));
        media.setAccessLevel(requestDto.getAccessLevel());
        media.setOwner(user);
        media.setOrganization(organization);
        media.setTarget(requestDto.getTarget());
        media.setIngestionStatus(IngestionStatus.PENDING);

        if (requestDto.getFolderId() != null) {
            MediaEntity parent = mediaRepository.findById(requestDto.getFolderId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_PARENT_NOT_EXISTS));
            if(parent.isFolder() == false){
                throw new AppException(ApiResponseStatus.MEDIA_PARENT_MUST_BE_FOLDER);
            }
            media.setParent(parent);
        }

        media = mediaRepository.save(media);

        // Nếu target không phải là INGESTION thì không cần đẩy sang ingestion service,
        // trả về response ngay
        if (!requestDto.getTarget().equals(MediaUploadTarget.INGESTION)) {
            return mediaMapper.entityToResponseDto(media);
        }

        try {
            IngestionUploadResponseDto ingestionResponse = ingestionService.pushToVector(
                    requestDto.getFile(),
                    user.getUserName(),
                    organization.getName(),
                    "public");

            // Nếu response từ ingestion service không hợp lệ thì đánh dấu media này là
            // failed để tránh trường hợp media bị treo ở trạng thái pending mãi mãi, đồng
            // thời trả về lỗi cho client để client có thể hiển thị thông báo lỗi chính xác
            if (ingestionResponse == null || ingestionResponse.getJobId() == null) {
                media.setIngestionStatus(IngestionStatus.FAILED);
                mediaRepository.save(media);
                throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
            }

            // Cập nhật jobId và trạng thái ingestion của media sau khi đã đẩy sang
            // ingestion service thành công
            media.setJobId(ingestionResponse.getJobId());
            media.setIngestionStatus(IngestionStatus.PENDING);
            media = mediaRepository.save(media);

            return mediaMapper.entityToResponseDto(media);

        } catch (AppException exception) {
            media.setIngestionStatus(IngestionStatus.FAILED);
            mediaRepository.save(media);
            throw exception;
        }
    }

    @Transactional
    public MediaResponseDto createFolder(MediaCreateFolderRequestDto requestDto) {
        UserEntity user = userService.getEntityById(JwtUtil.getUserId());
        OrganizationEntity organization = organizationService.getEntityById(JwtUtil.getOrgId());

        MediaEntity media = new MediaEntity();
        media.setName(requestDto.getName().trim());
        media.setFolder(true);
        media.setContentType("folder");
        media.setFileSize(0L);
        media.setAccessLevel(requestDto.getAccessLevel());
        media.setOwner(user);
        media.setOrganization(organization);
        media.setTarget(requestDto.getTarget());
        media.setJobId(null);
        media.setIngestionStatus(null);

        if (requestDto.getParentId() != null) {
            MediaEntity parent = mediaRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_PARENT_NOT_EXISTS));
            if(parent.isFolder() == false){
                throw new AppException(ApiResponseStatus.MEDIA_PARENT_MUST_BE_FOLDER);
            }
            media.setParent(parent);
        }

        return mediaMapper.entityToResponseDto(mediaRepository.save(media));
    }

    @Transactional(readOnly = true)
    public MediaResponseDto getById(UUID mediaId) {
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_NOT_EXISTS));
        return mediaMapper.entityToResponseDto(media);
    }

    @Transactional
    public MediaDownloadData downloadById(UUID mediaId) {
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_NOT_EXISTS));

        validateDownloadableMedia(media);

        MinioService.MinioObjectData objectData = minioService.download(media.getMinioPath());
        mediaRepository.save(media);

        return new MediaDownloadData(
                media.getName(),
                objectData.getContentType(),
                objectData.getBytes());
    }

    @Transactional(readOnly = true)
    public MediaPresignedUrlResponseDto getPresignedDownloadUrl(UUID mediaId, Integer expiresInSeconds) {
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_NOT_EXISTS));

        validateDownloadableMedia(media);

        int effectiveExpiry = expiresInSeconds == null || expiresInSeconds <= 0
                ? DEFAULT_PRESIGNED_EXPIRY_SECONDS
                : expiresInSeconds;

        String url = minioService.generatePresignedDownloadUrl(media.getMinioPath(), effectiveExpiry);
        return MediaPresignedUrlResponseDto.builder()
                .url(url)
                .expiresInSeconds(effectiveExpiry)
                .build();
    }

    @Transactional
    public MediaResponseDto updateFolder(UUID mediaId, MediaUpdateFolderRequestDto requestDto) {
        MediaEntity folder = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_NOT_EXISTS));

        if (!folder.isFolder()) {
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

            if (!parent.isFolder()) {
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

        return mediaMapper.entityToResponseDto(mediaRepository.save(folder));
    }

    @Transactional(readOnly = true)
    public Page<MediaResponseDto> getAll(MediaFilterDto filterDto) {
        return mediaRepository.findAll(filterDto.createSpec(), filterDto.createPageable())
                .map(mediaMapper::entityToResponseDto);
    }

    @Transactional(noRollbackFor = AppException.class)
    public MediaResponseDto retryIngestion(UUID mediaId) {
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_NOT_EXISTS));

        // Chỉ cho phép retry ingestion với media có target là INGESTION và có trạng thái ingestion là FAILED, đồng thời phải có minioPath hợp lệ để có thể retry upload lại lên ingestion service, tránh trường hợp người dùng tạo một media mới có
        if (media.getTarget() == null || !MediaUploadTarget.INGESTION.equals(media.getTarget())) {
            throw new AppException(ApiResponseStatus.MEDIA_INGESTION_RETRY_ONLY_INGESTION_TARGET);
        }

        // Kiểm tra trạng thái ingestion của media, chỉ cho phép retry nếu trạng thái là FAILED, tránh trường hợp người dùng cố tình retry với media đang ở trạng thái PENDING hoặc thậm chí là SUCCESS
        if (media.getIngestionStatus() == null || !IngestionStatus.FAILED.equals(media.getIngestionStatus())) {
            throw new AppException(ApiResponseStatus.MEDIA_INGESTION_RETRY_ONLY_FAILED);
        }

        // Kiểm tra xem media này có minioPath hợp lệ hay không, nếu không có thì không thể retry được vì không biết đường dẫn nào để lấy file lên ingestion service
        if (media.getMinioPath() == null || media.getMinioPath().isBlank()) {
            throw new AppException(ApiResponseStatus.MEDIA_UPLOAD_FAILED);
        }

        UserEntity owner = media.getOwner();
        OrganizationEntity organization = media.getOrganization();

        try {
            MinioService.MinioObjectData objectData = minioService.download(media.getMinioPath());
            IngestionUploadResponseDto ingestionResponse = ingestionService.pushToVector(
                    objectData.getBytes(),
                    media.getName(),
                    owner.getUserName(),
                    organization.getName(),
                    media.getAccessLevel().name());

            if (ingestionResponse == null || ingestionResponse.getJobId() == null) {
                media.setIngestionStatus(IngestionStatus.FAILED);
                mediaRepository.save(media);
                throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
            }

            media.setJobId(ingestionResponse.getJobId());
            media.setIngestionStatus(IngestionStatus.PENDING);
            mediaRepository.save(media);

            return mediaMapper.entityToResponseDto(media);
        } catch (AppException exception) {
            media.setIngestionStatus(IngestionStatus.FAILED);
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
            media.setIngestionStatus(IngestionStatus.valueOf(resolvedStatus));
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

    private void validateDownloadableMedia(MediaEntity media) {
        if (media.isFolder()) {
            throw new AppException(ApiResponseStatus.MEDIA_FOLDER_ONLY_OPERATION);
        }
        if (media.getMinioPath() == null || media.getMinioPath().isBlank()) {
            throw new AppException(ApiResponseStatus.MEDIA_DOWNLOAD_FAILED);
        }
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
