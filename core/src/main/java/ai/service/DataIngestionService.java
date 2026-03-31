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

import ai.constant.CacheName;
import ai.dto.outer.ingestion.response.IngestionStatusResponseDto;
import ai.dto.outer.ingestion.response.IngestionUploadResponseDto;
import ai.dto.own.request.DataIngestionCreateFolderRequestDto;
import ai.dto.own.request.DataIngestionUpdateFolderRequestDto;
import ai.dto.own.request.DataIngestionUploadRequestDto;
import ai.dto.own.request.filter.DataIngestionFilterDto;
import ai.dto.own.response.DataIngestionDownloadData;
import ai.dto.own.response.DataIngestionJobStatusResponseDto;
import ai.dto.own.response.DataIngestionPresignedUrlResponseDto;
import ai.dto.own.response.DataIngestionResponseDto;
import ai.entity.postgres.DataIngestionEntity;
import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.DataIngestionDeleteStatus;
import ai.enums.IngestionStatus;
import ai.exeption.AppException;
import ai.mapper.DataIngestionMapper;
import ai.repository.DataIngestionRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
@Service
public class DataIngestionService {
    final static int DEFAULT_PRESIGNED_EXPIRY_SECONDS = 900;
    
    boolean ingestionServiceAvailable = true;
    boolean dataIngestionDeleteWorkerAvailable = true;

    final DataIngestionRepository dataIngestionRepository;
    final IngestionService ingestionService;
    final MinioService minioService;
    final DataIngestionMapper dataIngestionMapper;
    final UserService userService;
    final OrganizationService organizationService;

    @Scheduled(cron = "0 0/1 * * * ?") // Mỗi 1 phút chạy một lần
    public void syncIngestionStatuses() {
        if (!ingestionServiceAvailable) {
            return;
        }
        ingestionServiceAvailable = false; // Đánh dấu đang trong quá trình đồng bộ để tránh nhiều instance cùng chạy song song
        System.out.println("Start syncing ingestion statuses for pending data ingestion items...");
        // Lấy tất cả data ingestion có target là INGESTION và trạng thái ingestion là PENDING để đồng bộ trạng thái mới nhất từ ingestion service
        dataIngestionRepository.findByIngestionStatus(IngestionStatus.PENDING)
                .forEach(dataIngestion -> {
                    try {
                        pollIngestionJobStatus(dataIngestion.getId());
                    } catch (Exception exception) {
                        // Log lỗi và tiếp tục đồng bộ các item còn lại để không chặn toàn bộ tiến trình
                        System.err.println("Error syncing ingestion status for data ingestion with ID: " + dataIngestion.getId());
                        exception.printStackTrace();
                    }
                });
        ingestionServiceAvailable = true;
        System.out.println("Finished syncing ingestion statuses for pending data ingestion items.");
    }

    @Scheduled(cron = "0 0/1 * * * ?") // Mỗi 1 phút chạy một lần
    public void processPendingDataIngestionDeletes() {
        if (!dataIngestionDeleteWorkerAvailable) {
            return;
        }

        dataIngestionDeleteWorkerAvailable = false;
        System.out.println("Start processing pending data ingestion deletions...");
        try {
            dataIngestionRepository.findByDeleteStatus(DataIngestionDeleteStatus.PENDING_DELETE)
                    .forEach(dataIngestion -> {
                        try {
                            executeDelete(dataIngestion);
                        } catch (Exception exception) {
                            // Log lỗi và tiếp tục xử lý các item còn lại
                            System.err.println("Error processing delete for data ingestion with ID: " + dataIngestion.getId());
                            exception.printStackTrace();
                        }
                    });
        } finally {
            dataIngestionDeleteWorkerAvailable = true;
            System.out.println("Finished processing pending data ingestion deletions.");
        }
    }

    @Transactional(noRollbackFor = AppException.class)
    public DataIngestionResponseDto uploadDataIngestion(DataIngestionUploadRequestDto requestDto) {
        UserEntity user = userService.getEntityById(JwtUtil.getUserId());
        OrganizationEntity organization = organizationService.getEntityById(JwtUtil.getOrgId());

        // Push lên MinIO trước để tránh trường hợp đã lưu data ingestion vào database nhưng
        // upload file lên MinIO thất bại, dẫn đến dữ liệu bị lỗi không thể retry
        // ingestion được
        String minioPath = minioService.upload(requestDto.getFile(), user.getUserName(), organization.getName());

        DataIngestionEntity dataIngestion = new DataIngestionEntity();
        dataIngestion.setName(requestDto.getFile().getOriginalFilename());
        dataIngestion.setFolder(false);
        dataIngestion.setMinioPath(minioPath);
        dataIngestion.setFileSize(requestDto.getFile().getSize());
        dataIngestion.setContentType(requestDto.getFile().getContentType());
        dataIngestion.setAccessLevel(requestDto.getAccessLevel());
        dataIngestion.setOwner(user);
        dataIngestion.setOrganization(organization);
        dataIngestion.setIngestionStatus(IngestionStatus.PENDING);
        dataIngestion.setDeleteStatus(DataIngestionDeleteStatus.ACTIVE);

        if (requestDto.getFolderId() != null) {
            DataIngestionEntity parent = dataIngestionRepository.findById(requestDto.getFolderId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_PARENT_NOT_EXISTS));
            if(parent.isFolder() == false){
                throw new AppException(ApiResponseStatus.DATA_INGESTION_PARENT_MUST_BE_FOLDER);
            }
            dataIngestion.setParent(parent);
        }

        dataIngestion = dataIngestionRepository.save(dataIngestion);

        try {
            IngestionUploadResponseDto ingestionResponse = ingestionService.pushToVector(
                    requestDto.getFile(),
                    dataIngestion.getId().toString(),
                    user.getUserName(),
                    organization.getId().toString(),
                    organization.getName(),
                    dataIngestion.getAccessLevel());

            // Nếu response từ ingestion service không hợp lệ thì đánh dấu data ingestion này là
            // failed để tránh trường hợp dữ liệu bị treo ở trạng thái pending mãi mãi, đồng
            // thời trả về lỗi cho client để client có thể hiển thị thông báo lỗi chính xác
            if (ingestionResponse == null || ingestionResponse.getJobId() == null) {
                dataIngestion.setIngestionStatus(IngestionStatus.FAILED);
                dataIngestionRepository.save(dataIngestion);

                return dataIngestionMapper.entityToResponseDto(dataIngestion);
            }

            // Cập nhật jobId và trạng thái ingestion sau khi đã đẩy sang
            // ingestion service thành công
            dataIngestion.setJobId(ingestionResponse.getJobId());
            dataIngestion.setIngestionStatus(IngestionStatus.PENDING);
            dataIngestion = dataIngestionRepository.save(dataIngestion);

            return dataIngestionMapper.entityToResponseDto(dataIngestion);

        } catch (AppException exception) {
            exception.printStackTrace();
            dataIngestion.setIngestionStatus(IngestionStatus.FAILED);
            dataIngestionRepository.save(dataIngestion);
            return dataIngestionMapper.entityToResponseDto(dataIngestion);
        }
    }

    @Transactional
    public DataIngestionResponseDto createFolder(DataIngestionCreateFolderRequestDto requestDto) {
        UserEntity user = userService.getEntityById(JwtUtil.getUserId());
        OrganizationEntity organization = organizationService.getEntityById(JwtUtil.getOrgId());

        DataIngestionEntity dataIngestion = new DataIngestionEntity();
        dataIngestion.setName(requestDto.getName().trim());
        dataIngestion.setFolder(true);
        dataIngestion.setContentType(null);
        dataIngestion.setFileSize(0L);
        dataIngestion.setAccessLevel(null);
        dataIngestion.setOwner(user);
        dataIngestion.setOrganization(organization);
        dataIngestion.setJobId(null);
        dataIngestion.setIngestionStatus(null);
        dataIngestion.setDeleteStatus(DataIngestionDeleteStatus.ACTIVE);

        if (requestDto.getParentId() != null) {
            DataIngestionEntity parent = dataIngestionRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_PARENT_NOT_EXISTS));
            if(parent.isFolder() == false){
                throw new AppException(ApiResponseStatus.DATA_INGESTION_PARENT_MUST_BE_FOLDER);
            }
            dataIngestion.setParent(parent);
        }
        dataIngestion = dataIngestionRepository.save(dataIngestion);

        return dataIngestionMapper.entityToResponseDto(dataIngestion);
    }

    @Cacheable(value = CacheName.DATA_INGESTION_DTO_DETAILS, key = "#dataIngestionId", unless = "#result == null", condition = "#dataIngestionId != null")
    @Transactional(readOnly = true)
    public DataIngestionResponseDto getById(UUID dataIngestionId) {
        DataIngestionEntity dataIngestion = dataIngestionRepository.findById(dataIngestionId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));
        return dataIngestionMapper.entityToResponseDto(dataIngestion);
    }

    @Transactional
    public DataIngestionDownloadData downloadById(UUID dataIngestionId) {
        DataIngestionEntity dataIngestion = dataIngestionRepository.findById(dataIngestionId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));

        validateDownloadableDataIngestion(dataIngestion);

        MinioService.MinioObjectData objectData = minioService.download(dataIngestion.getMinioPath());
        dataIngestionRepository.save(dataIngestion);

        return new DataIngestionDownloadData(
            dataIngestion.getName(),
                objectData.getContentType(),
                objectData.getBytes());
    }

    @Transactional(readOnly = true)
    public DataIngestionPresignedUrlResponseDto getPresignedDownloadUrl(UUID dataIngestionId, Integer expiresInSeconds) {
        DataIngestionEntity dataIngestion = dataIngestionRepository.findById(dataIngestionId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));

        validateDownloadableDataIngestion(dataIngestion);

        int effectiveExpiry = expiresInSeconds == null || expiresInSeconds <= 0
                ? DEFAULT_PRESIGNED_EXPIRY_SECONDS
                : expiresInSeconds;

        String url = minioService.generatePresignedDownloadUrl(dataIngestion.getMinioPath(), effectiveExpiry);
        return DataIngestionPresignedUrlResponseDto.builder()
                .url(url)
                .expiresInSeconds(effectiveExpiry)
                .build();
    }

    @CacheEvict(value = CacheName.DATA_INGESTION_DTO_DETAILS, key = "#dataIngestionId", condition = "#dataIngestionId != null")
    @Transactional
    public DataIngestionResponseDto updateFolder(UUID dataIngestionId, DataIngestionUpdateFolderRequestDto requestDto) {
        DataIngestionEntity folder = dataIngestionRepository.findById(dataIngestionId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));

        if (!folder.isFolder()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_ONLY_OPERATION);
        }

        boolean hasName = requestDto != null && requestDto.getName() != null && !requestDto.getName().isBlank();
        boolean hasParent = requestDto != null && requestDto.getParentId() != null;
        boolean moveToRoot = requestDto != null && Boolean.TRUE.equals(requestDto.getMoveToRoot());
        if (!hasName && !hasParent && !moveToRoot) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_UPDATE_REQUIRED);
        }

        // Đổi tên
        if (hasName) {
            folder.setName(requestDto.getName().trim());
        }

        // Di chuyển thư mục
        if (moveToRoot) {
            folder.setParent(null);
        } else if (hasParent) {
            DataIngestionEntity parent = dataIngestionRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_PARENT_NOT_EXISTS));

            if (!parent.isFolder()) {
                throw new AppException(ApiResponseStatus.DATA_INGESTION_PARENT_MUST_BE_FOLDER);
            }
            if (folder.getId().equals(parent.getId())) {
                throw new AppException(ApiResponseStatus.DATA_INGESTION_MOVE_CYCLE_NOT_ALLOWED);
            }
            if (isDescendantOf(parent, folder)) {
                throw new AppException(ApiResponseStatus.DATA_INGESTION_MOVE_CYCLE_NOT_ALLOWED);
            }

            folder.setParent(parent);
        }

        return dataIngestionMapper.entityToResponseDto(dataIngestionRepository.save(folder));
    }

    @Transactional(readOnly = true)
    public Page<DataIngestionResponseDto> getAll(DataIngestionFilterDto filterDto) {
        return dataIngestionRepository.findAll(filterDto.createSpec(), filterDto.createPageable())
                .map(dataIngestionMapper::entityToResponseDto);
    }

    @Transactional(noRollbackFor = AppException.class)
    public DataIngestionResponseDto retryIngestion(UUID dataIngestionId) {
        DataIngestionEntity dataIngestion = dataIngestionRepository.findById(dataIngestionId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));

        // Không cho phép nếu là folder
        if (dataIngestion.isFolder()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_ONLY_OPERATION);
        }

        // Không cho phép retry ingestion với data ingestion đang ở trạng thái pending delete
        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(dataIngestion))) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DELETE_IN_PROGRESS);
        }

        // Chỉ cho phép retry nếu trạng thái ingestion hiện tại là FAILED
        if (dataIngestion.getIngestionStatus() == null || !IngestionStatus.FAILED.equals(dataIngestion.getIngestionStatus())) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_RETRY_ONLY_FAILED);
        }

        // Kiểm tra minioPath hợp lệ trước khi retry
        if (dataIngestion.getMinioPath() == null || dataIngestion.getMinioPath().isBlank()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_UPLOAD_FAILED);
        }

        UserEntity owner = dataIngestion.getOwner();
        OrganizationEntity organization = dataIngestion.getOrganization();

        try {
            MinioService.MinioObjectData objectData = minioService.download(dataIngestion.getMinioPath());
            IngestionUploadResponseDto ingestionResponse = ingestionService.pushToVector(
                    objectData.getBytes(),
                    dataIngestion.getName(),
                    dataIngestion.getId().toString(),
                    owner.getUserName(),
                    organization.getId().toString(),
                    organization.getName(),
                    dataIngestion.getAccessLevel());
            System.out.println("Ingestion response after retrying ingestion for data ingestion with ID " + dataIngestionId + ": " + ingestionResponse);

             // Nếu response từ ingestion service không hợp lệ thì đánh dấu dữ liệu này là failed để tránh bị treo ở trạng thái pending mãi mãi

            if (ingestionResponse == null || ingestionResponse.getJobId() == null) {
                dataIngestion.setIngestionStatus(IngestionStatus.FAILED);
                dataIngestionRepository.save(dataIngestion);
                throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
            }

            dataIngestion.setJobId(ingestionResponse.getJobId());
            dataIngestion.setIngestionStatus(IngestionStatus.PENDING);
            dataIngestionRepository.save(dataIngestion);

            return dataIngestionMapper.entityToResponseDto(dataIngestion);
        } catch (AppException exception) {
            exception.printStackTrace();
            dataIngestion.setIngestionStatus(IngestionStatus.FAILED);
            dataIngestionRepository.save(dataIngestion);
            throw exception;
        }
    }

    @Transactional
    public DataIngestionJobStatusResponseDto pollIngestionJobStatus(UUID dataIngestionId) {
        Optional<DataIngestionEntity> dataIngestionOptional = dataIngestionRepository.findById(dataIngestionId);
        if (dataIngestionOptional.isEmpty()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS);
        }

        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(dataIngestionOptional.get()))) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DELETE_IN_PROGRESS);
        }
        
        if(dataIngestionOptional.get().getJobId() == null) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_JOB_ID_NOT_EXISTS);
        }

        // Lấy trạng thái ingestion mới nhất từ ingestion service
        IngestionStatusResponseDto ingestionStatusResponse = ingestionService.getJobStatus(dataIngestionOptional.get().getJobId());
        String resolvedStatus = ingestionStatusResponse.getStatus();
        if (resolvedStatus == null || resolvedStatus.isBlank()) {
            resolvedStatus = IngestionStatus.PENDING.name();
        } else {
            resolvedStatus = resolvedStatus.trim().toUpperCase(Locale.ROOT);
        }

        DataIngestionEntity dataIngestion = dataIngestionOptional.get();
        if(dataIngestion.getIngestionStatus() == null || !dataIngestion.getIngestionStatus().name().equals(resolvedStatus)) {
            dataIngestion.setIngestionStatus(IngestionStatus.valueOf(resolvedStatus));
            dataIngestionRepository.save(dataIngestion);
        }

        return DataIngestionJobStatusResponseDto.builder()
                .dataIngestionId(dataIngestion.getId())
                .jobId(dataIngestion.getJobId())
                .ingestionStatus(resolvedStatus)
                .message(ingestionStatusResponse.getMessage())
                .build();
    }


    @CacheEvict(value = CacheName.DATA_INGESTION_DTO_DETAILS, key = "#dataIngestionId", condition = "#dataIngestionId != null")
    @Transactional
    public DataIngestionResponseDto deleteFileById(UUID dataIngestionId) {
        DataIngestionEntity dataIngestion = dataIngestionRepository.findById(dataIngestionId).orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));

        // Nếu data ingestion là folder thì không cho phép xóa bằng API file
        if (dataIngestion.isFolder()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_ONLY_OPERATION);
        }

        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(dataIngestion))) {
            return dataIngestionMapper.entityToResponseDto(dataIngestion);
        }

        dataIngestion.setDeleteStatus(DataIngestionDeleteStatus.PENDING_DELETE);
        dataIngestion = dataIngestionRepository.save(dataIngestion);

        return dataIngestionMapper.entityToResponseDto(dataIngestion);
    }

    @CacheEvict(value = CacheName.DATA_INGESTION_DTO_DETAILS, key = "#dataIngestionId", condition = "#dataIngestionId != null")
    @Transactional
    public DataIngestionResponseDto deleteById(UUID dataIngestionId) {
        return deleteFileById(dataIngestionId);
    }

    private void executeDelete(DataIngestionEntity dataIngestion) {
        try {
            // Xóa bên minio trước để tránh rác file nếu xóa database thành công nhưng xóa object thất bại
            if (dataIngestion.getMinioPath() != null && !dataIngestion.getMinioPath().isBlank()) {
                minioService.delete(dataIngestion.getMinioPath());
            }

            // Nếu data ingestion này liên quan đến ingestion job nào đó thì gọi API của ingestion service để xóa job đó luôn, tránh trường hợp dữ liệu bị xóa nhưng job
            if (dataIngestion.getJobId() != null) {
                ingestionService.deleteFile(dataIngestion.getId().toString());
            }

            dataIngestionRepository.delete(dataIngestion);
        } catch (Exception exception) {
            dataIngestionRepository.findById(dataIngestion.getId()).ifPresent(entity -> {
                entity.setDeleteStatus(DataIngestionDeleteStatus.DELETE_FAILED);
                dataIngestionRepository.save(entity);
            });
            throw exception;
        }
    }

    @CacheEvict(value = CacheName.DATA_INGESTION_DTO_DETAILS, key = "#dataIngestionId", condition = "#dataIngestionId != null")
    @Transactional
    public void deleteFolderById(UUID dataIngestionId) {
        DataIngestionEntity dataIngestion = dataIngestionRepository.findById(dataIngestionId).orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));
        if (!dataIngestion.isFolder()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_ONLY_OPERATION);
        }
        
        dataIngestionRepository.delete(dataIngestion);
    }

    private void validateDownloadableDataIngestion(DataIngestionEntity dataIngestion) {
        if (dataIngestion.isFolder()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_ONLY_OPERATION);
        }
        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(dataIngestion))) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DELETE_IN_PROGRESS);
        }
        if (dataIngestion.getMinioPath() == null || dataIngestion.getMinioPath().isBlank()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DOWNLOAD_FAILED);
        }
    }

    private DataIngestionDeleteStatus resolveDeleteStatus(DataIngestionEntity dataIngestion) {
        return dataIngestion.getDeleteStatus() == null ? DataIngestionDeleteStatus.ACTIVE : dataIngestion.getDeleteStatus();
    }

    private boolean isDescendantOf(DataIngestionEntity candidateParent, DataIngestionEntity node) {
        DataIngestionEntity cursor = candidateParent;
        while (cursor != null) {
            if (cursor.getId().equals(node.getId())) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }
}
