package ai.service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
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
import ai.enums.CacheNames;
import ai.enums.IngestionStatus;
import ai.enums.MediaDeleteStatus;
import ai.enums.MediaUploadTarget;
import ai.exeption.AppException;
import ai.mapper.MediaMapper;
import ai.repository.MediaRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
@Service
public class MediaService {
    final static int DEFAULT_PRESIGNED_EXPIRY_SECONDS = 900;
    
    boolean ingestionServiceAvailable = true;
    boolean mediaDeleteWorkerAvailable = true;

    final MediaRepository mediaRepository;
    final IngestionService ingestionService;
    final MinioService minioService;
    final MediaMapper mediaMapper;
    final UserService userService;
    final OrganizationService organizationService;

    @Scheduled(cron = "0 0/1 * * * ?") // Mỗi 1 phút chạy một lần
    public void syncIngestionStatuses() {
        if (!ingestionServiceAvailable) {
            return;
        }
        ingestionServiceAvailable = false; // Đánh dấu đang trong quá trình đồng bộ để tránh trường hợp có nhiều instance của media service cùng đồng bộ một lúc, dẫn đến tình trạng thừa thãi và có thể gây ra xung đột
        System.out.println("Start syncing ingestion statuses for pending media items...");
        // Lấy tất cả media có target là INGESTION và trạng thái ingestion là PENDING để đồng bộ trạng thái mới nhất từ ingestion service, tránh trường hợp người dùng có thể thấy trạng thái đã xử lý xong ở ingestion service nhưng vẫn thấy media này ở trạng thái PENDING trong hệ thống của mình
        mediaRepository.findByTargetAndIngestionStatus(MediaUploadTarget.INGESTION, IngestionStatus.PENDING)
                .forEach(media -> {
                    try {
                        pollIngestionJobStatus(media.getId());
                    } catch (Exception exception) {
                        // Log lỗi và tiếp tục đồng bộ các media còn lại, tránh trường hợp một lỗi ở ingestion service làm gián đoạn toàn bộ quá trình đồng bộ
                        System.err.println("Error syncing ingestion status for media with ID: " + media.getId());
                        exception.printStackTrace();
                    }
                });
        ingestionServiceAvailable = true; // Đồng bộ xong thì đánh dấu là đã sẵn sàng để đồng bộ lần tiếp theo
        System.out.println("Finished syncing ingestion statuses for pending media items.");
    }

    @Scheduled(cron = "0 0/1 * * * ?") // Mỗi 1 phút chạy một lần
    public void processPendingMediaDeletes() {
        if (!mediaDeleteWorkerAvailable) {
            return;
        }

        mediaDeleteWorkerAvailable = false;
        System.out.println("Start processing pending media deletions...");
        try {
            mediaRepository.findByDeleteStatus(MediaDeleteStatus.PENDING_DELETE)
                    .forEach(media -> {
                        try {
                            executeDelete(media);
                        } catch (Exception exception) {
                            // Log lỗi và tiếp tục xử lý các media còn lại
                            System.err.println("Error processing delete for media with ID: " + media.getId());
                            exception.printStackTrace();
                        }
                    });
        } finally {
            mediaDeleteWorkerAvailable = true;
            System.out.println("Finished processing pending media deletions.");
        }
    }

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
        media.setContentType(requestDto.getFile().getContentType());
        media.setAccessLevel(requestDto.getAccessLevel());
        media.setOwner(user);
        media.setOrganization(organization);
        media.setTarget(requestDto.getTarget());
        media.setIngestionStatus(IngestionStatus.PENDING);
        media.setDeleteStatus(MediaDeleteStatus.ACTIVE);

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
                    media.getId().toString(),
                    user.getUserName(),
                    organization.getId().toString(),
                    organization.getName(),
                    media.getAccessLevel());

            // Nếu response từ ingestion service không hợp lệ thì đánh dấu media này là
            // failed để tránh trường hợp media bị treo ở trạng thái pending mãi mãi, đồng
            // thời trả về lỗi cho client để client có thể hiển thị thông báo lỗi chính xác
            if (ingestionResponse == null || ingestionResponse.getJobId() == null) {
                media.setIngestionStatus(IngestionStatus.FAILED);
                mediaRepository.save(media);

                return mediaMapper.entityToResponseDto(media);
            }

            // Cập nhật jobId và trạng thái ingestion của media sau khi đã đẩy sang
            // ingestion service thành công
            media.setJobId(ingestionResponse.getJobId());
            media.setIngestionStatus(IngestionStatus.PENDING);
            media = mediaRepository.save(media);

            return mediaMapper.entityToResponseDto(media);

        } catch (AppException exception) {
            exception.printStackTrace();
            media.setIngestionStatus(IngestionStatus.FAILED);
            mediaRepository.save(media);
            return mediaMapper.entityToResponseDto(media);
        }
    }

    @Transactional
    public MediaResponseDto createFolder(MediaCreateFolderRequestDto requestDto) {
        UserEntity user = userService.getEntityById(JwtUtil.getUserId());
        OrganizationEntity organization = organizationService.getEntityById(JwtUtil.getOrgId());

        MediaEntity media = new MediaEntity();
        media.setName(requestDto.getName().trim());
        media.setFolder(true);
        media.setContentType(null);
        media.setFileSize(0L);
        media.setAccessLevel(null);
        media.setOwner(user);
        media.setOrganization(organization);
        media.setTarget(null);
        media.setJobId(null);
        media.setIngestionStatus(null);
        media.setDeleteStatus(MediaDeleteStatus.ACTIVE);

        if (requestDto.getParentId() != null) {
            MediaEntity parent = mediaRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_PARENT_NOT_EXISTS));
            if(parent.isFolder() == false){
                throw new AppException(ApiResponseStatus.MEDIA_PARENT_MUST_BE_FOLDER);
            }
            media.setParent(parent);
        }
        media = mediaRepository.save(media);

        return mediaMapper.entityToResponseDto(media);
    }

    @Cacheable(value = CacheNames.MEDIA_DTO_DETAILS, key = "#mediaId", unless = "#result == null", condition = "#mediaId != null")
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

    @CacheEvict(value = CacheNames.MEDIA_DTO_DETAILS, key = "#mediaId", condition = "#mediaId != null")
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

        // Đổi tên
        if (hasName) {
            folder.setName(requestDto.getName().trim());
        }

        // Di chuyển thư mục
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

        // Không cho phép nếu là folder
        if (media.isFolder()) {
            throw new AppException(ApiResponseStatus.MEDIA_FOLDER_ONLY_OPERATION);
        }

        // Không cho phép retry ingestion với media đang ở trạng thái pending delete để tránh trường hợp người dùng cố tình retry ingestion với một media đang chờ xóa, dẫn đến việc media này bị treo ở trạng thái pending mãi mãi mà không thể xóa được
        if (MediaDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(media))) {
            throw new AppException(ApiResponseStatus.MEDIA_DELETE_IN_PROGRESS);
        }

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
                    media.getId().toString(),
                    owner.getUserName(),
                    organization.getId().toString(),
                    organization.getName(),
                    media.getAccessLevel());
            System.out.println("Ingestion response after retrying ingestion for media with ID " + mediaId + ": " + ingestionResponse);

             // Nếu response từ ingestion service không hợp lệ thì đánh dấu media này là failed để tránh trường hợp media bị treo ở trạng thái pending mãi mãi, đồng thời trả về lỗi cho client để client có thể hiển thị thông báo lỗi chính xác

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
            exception.printStackTrace();
            media.setIngestionStatus(IngestionStatus.FAILED);
            mediaRepository.save(media);
            throw exception;
        }
    }

    @Transactional
    public MediaJobStatusResponseDto pollIngestionJobStatus(UUID mediaId) {
        Optional<MediaEntity> mediaOptional = mediaRepository.findById(mediaId);
        if (mediaOptional.isEmpty()) {
            throw new AppException(ApiResponseStatus.MEDIA_NOT_EXISTS);
        }

        if (MediaDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(mediaOptional.get()))) {
            throw new AppException(ApiResponseStatus.MEDIA_DELETE_IN_PROGRESS);
        }
        
        if(mediaOptional.get().getJobId() == null) {
            throw new AppException(ApiResponseStatus.MEDIA_JOB_ID_NOT_EXISTS);
        }

        // Lấy trạng thái ingestion mới nhất từ ingestion service
        IngestionStatusResponseDto ingestionStatusResponse = ingestionService.getJobStatus(mediaOptional.get().getJobId());
        String resolvedStatus = ingestionStatusResponse.getStatus();
        if (resolvedStatus == null || resolvedStatus.isBlank()) {
            resolvedStatus = IngestionStatus.PENDING.name();
        } else {
            resolvedStatus = resolvedStatus.trim().toUpperCase(Locale.ROOT);
        }

        MediaEntity media = mediaOptional.get();
        if(media.getIngestionStatus() == null || !media.getIngestionStatus().name().equals(resolvedStatus)) {
            media.setIngestionStatus(IngestionStatus.valueOf(resolvedStatus));
            mediaRepository.save(media);
        }

        return MediaJobStatusResponseDto.builder()
                .mediaId(media.getId())
                .jobId(media.getJobId())
                .ingestionStatus(resolvedStatus)
                .message(ingestionStatusResponse.getMessage())
                .build();
    }


    @CacheEvict(value = CacheNames.MEDIA_DTO_DETAILS, key = "#mediaId", condition = "#mediaId != null")
    @Transactional
    public MediaResponseDto deleteFileById(UUID mediaId) {
        MediaEntity media = mediaRepository.findById(mediaId).orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_NOT_EXISTS));

        // Nếu media có phải là folder thì không cho phép xóa để tránh trường hợp xóa nhầm cả một cây media con bên dưới, bắt buộc phải xóa hết media con trước rồi mới xóa được folder
        if (media.isFolder()) {
            throw new AppException(ApiResponseStatus.MEDIA_FOLDER_ONLY_OPERATION);
        }

        if (MediaDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(media))) {
            return mediaMapper.entityToResponseDto(media);
        }

        media.setDeleteStatus(MediaDeleteStatus.PENDING_DELETE);
        media = mediaRepository.save(media);

        return mediaMapper.entityToResponseDto(media);
    }

    @CacheEvict(value = CacheNames.MEDIA_DTO_DETAILS, key = "#mediaId", condition = "#mediaId != null")
    @Transactional
    public MediaResponseDto deleteById(UUID mediaId) {
        return deleteFileById(mediaId);
    }

    private void executeDelete(MediaEntity media) {
        try {
            // Xóa bên minio trước để tránh trường hợp đã xóa media trong database nhưng xóa file trên minio thất bại, dẫn đến rác file trên minio
            if (media.getMinioPath() != null && !media.getMinioPath().isBlank()) {
                minioService.delete(media.getMinioPath());
            }

            // Nếu là ingestion media thì không chỉ xóa file trên minio mà còn phải xóa cả record ingestion job liên quan để tránh trường hợp media này bị treo ở trạng thái đã xóa trên database nhưng vẫn còn job ở
            // ingestion service, dẫn đến việc người dùng không thể upload lại một media mới được vì jobId của media mới bị trùng với jobId của media cũ đã bị xóa nhưng vẫn còn ở ingestion service
            if (MediaUploadTarget.INGESTION.equals(media.getTarget()) && media.getJobId() != null) {
                ingestionService.deleteFile(media.getId().toString());
            }

            // Sau khi đã xóa file trên minio thành công (hoặc nếu media này không có file trên minio) thì mới xóa record media trong database để tránh trường hợp media bị treo ở trạng thái đã xóa trên database nhưng file vẫn còn
            mediaRepository.delete(media);
        } catch (Exception exception) {
            mediaRepository.findById(media.getId()).ifPresent(entity -> {
                entity.setDeleteStatus(MediaDeleteStatus.DELETE_FAILED);
                mediaRepository.save(entity);
            });
            throw exception;
        }
    }

    @CacheEvict(value = CacheNames.MEDIA_DTO_DETAILS, key = "#mediaId", condition = "#mediaId != null")
    @Transactional
    public void deleteFolderById(UUID mediaId) {
        MediaEntity media = mediaRepository.findById(mediaId).orElseThrow(() -> new AppException(ApiResponseStatus.MEDIA_NOT_EXISTS));
        if (!media.isFolder()) {
            throw new AppException(ApiResponseStatus.MEDIA_FOLDER_ONLY_OPERATION);
        }
        
        mediaRepository.delete(media);
    }

    private void validateDownloadableMedia(MediaEntity media) {
        if (media.isFolder()) {
            throw new AppException(ApiResponseStatus.MEDIA_FOLDER_ONLY_OPERATION);
        }
        if (MediaDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(media))) {
            throw new AppException(ApiResponseStatus.MEDIA_DELETE_IN_PROGRESS);
        }
        if (media.getMinioPath() == null || media.getMinioPath().isBlank()) {
            throw new AppException(ApiResponseStatus.MEDIA_DOWNLOAD_FAILED);
        }
    }

    private MediaDeleteStatus resolveDeleteStatus(MediaEntity media) {
        return media.getDeleteStatus() == null ? MediaDeleteStatus.ACTIVE : media.getDeleteStatus();
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
}
