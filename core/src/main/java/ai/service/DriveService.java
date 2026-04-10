package ai.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.AppProperties;
import ai.dto.own.request.DriveCreateFolderRequestDto;
import ai.dto.own.request.DriveUpdateFolderRequestDto;
import ai.dto.own.request.DriveUploadRequestDto;
import ai.dto.own.request.filter.DriveFilterDto;
import ai.dto.own.response.DriveDownloadData;
import ai.dto.own.response.DrivePresignedUrlResponseDto;
import ai.dto.own.response.DriveResponseDto;
import ai.dto.own.response.DriveTreeResponseDto;
import ai.entity.postgres.DriveEntity;
import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.DataIngestionDeleteStatus;
import ai.exeption.AppException;
import ai.mapper.DriveMapper;
import ai.repository.DriveRepository;
import ai.util.JwtUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class DriveService {
    static int DEFAULT_PRESIGNED_EXPIRY_SECONDS = 900;
    static String DEFAULT_NOTEBOOK_BUCKET = "notebookllm";

    DriveRepository driveRepository;
    MinioService minioService;
    DriveMapper driveMapper;
    UserService userService;
    OrganizationService organizationService;
    AppProperties appProperties;

    @Transactional
    public DriveResponseDto upload(DriveUploadRequestDto requestDto) {
        UserEntity user = userService.getEntityById(JwtUtil.getUserId());
        OrganizationEntity organization = organizationService.getEntityById(JwtUtil.getOrgId());

        DriveEntity parent = null;
        if (requestDto.getFolderId() != null) {
            parent = driveRepository.findById(requestDto.getFolderId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.DRIVE_PARENT_NOT_EXISTS));
            validateOwnership(parent, user.getId(), organization.getId());
            if (!parent.isFolder()) {
                throw new AppException(ApiResponseStatus.DRIVE_PARENT_MUST_BE_FOLDER);
            }
            if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(parent))) {
                throw new AppException(ApiResponseStatus.DRIVE_DELETE_IN_PROGRESS);
            }
        }

        String minioPath;
        try {
            minioPath = minioService.upload(
                    requestDto.getFile(),
                    user.getUserName(),
                    organization.getName(),
                    resolveNotebookBucket());
        } catch (Exception exception) {
            throw new AppException(ApiResponseStatus.DRIVE_UPLOAD_FAILED);
        }

        DriveEntity drive = new DriveEntity();
        drive.setName(requestDto.getFile().getOriginalFilename());
        drive.setFolder(false);
        drive.setMinioPath(minioPath);
        drive.setFileSize(requestDto.getFile().getSize());
        drive.setContentType(requestDto.getFile().getContentType());
        drive.setOwner(user);
        drive.setOrganization(organization);
        drive.setDeleteStatus(DataIngestionDeleteStatus.ACTIVE);
        drive.setParent(parent);

        drive = driveRepository.save(drive);
        return driveMapper.entityToResponseDto(drive);
    }

    @Transactional
    public DriveResponseDto createFolder(DriveCreateFolderRequestDto requestDto) {
        UserEntity user = userService.getEntityById(JwtUtil.getUserId());
        OrganizationEntity organization = organizationService.getEntityById(JwtUtil.getOrgId());

        DriveEntity drive = new DriveEntity();
        drive.setName(requestDto.getName().trim());
        drive.setFolder(true);
        drive.setContentType(null);
        drive.setFileSize(0L);
        drive.setMinioPath(null);
        drive.setOwner(user);
        drive.setOrganization(organization);
        drive.setDeleteStatus(DataIngestionDeleteStatus.ACTIVE);

        if (requestDto.getParentId() != null) {
            DriveEntity parent = driveRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.DRIVE_PARENT_NOT_EXISTS));
            validateOwnership(parent, user.getId(), organization.getId());
            if (!parent.isFolder()) {
                throw new AppException(ApiResponseStatus.DRIVE_PARENT_MUST_BE_FOLDER);
            }
            if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(parent))) {
                throw new AppException(ApiResponseStatus.DRIVE_DELETE_IN_PROGRESS);
            }
            drive.setParent(parent);
        }

        drive = driveRepository.save(drive);
        return driveMapper.entityToResponseDto(drive);
    }

    @Transactional(readOnly = true)
    public DriveResponseDto getById(UUID driveId) {
        DriveEntity drive = getOwnedDriveById(driveId);
        return driveMapper.entityToResponseDto(drive);
    }

    @Transactional(readOnly = true)
    public Page<DriveResponseDto> getAll(DriveFilterDto filterDto) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        Specification<DriveEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) -> {
            Predicate orgIdPredicate = criteriaBuilder.equal(root.get("organization").get("id"), orgId);
            Predicate ownerPredicate = criteriaBuilder.equal(root.get("owner").get("id"), userId);
            Predicate activePredicate = criteriaBuilder.equal(root.get("deleteStatus"), DataIngestionDeleteStatus.ACTIVE);
            return criteriaBuilder.and(orgIdPredicate, ownerPredicate, activePredicate);
        });

        return driveRepository.findAll(spec, filterDto.createPageable())
                .map(driveMapper::entityToResponseDto);
    }

    @Transactional(readOnly = true)
    public List<DriveTreeResponseDto> getInfo() {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        List<DriveEntity> entities = driveRepository.findByOrganizationIdAndOwnerIdAndDeleteStatus(
                orgId,
                userId,
                DataIngestionDeleteStatus.ACTIVE);

        Map<UUID, DriveTreeResponseDto> indexedNodes = new LinkedHashMap<>();
        for (DriveEntity entity : entities) {
            DriveResponseDto responseDto = driveMapper.entityToResponseDto(entity);
            indexedNodes.put(entity.getId(), toTreeNode(responseDto));
        }

        List<DriveTreeResponseDto> roots = new ArrayList<>();
        for (DriveEntity entity : entities) {
            DriveTreeResponseDto current = indexedNodes.get(entity.getId());
            UUID parentId = entity.getParent() == null ? null : entity.getParent().getId();

            if (parentId == null || !indexedNodes.containsKey(parentId)) {
                roots.add(current);
                continue;
            }

            indexedNodes.get(parentId).getChildren().add(current);
        }

        sortTree(roots);
        return roots;
    }

    @Transactional
    public DriveDownloadData downloadById(UUID driveId) {
        DriveEntity drive = getOwnedDriveById(driveId);
        validateDownloadableDrive(drive);

        MinioService.MinioObjectData objectData;
        try {
            objectData = minioService.download(drive.getMinioPath(), resolveNotebookBucket());
        } catch (Exception exception) {
            throw new AppException(ApiResponseStatus.DRIVE_DOWNLOAD_FAILED);
        }
        return new DriveDownloadData(
                drive.getName(),
                objectData.getContentType(),
                objectData.getBytes());
    }

    @Transactional(readOnly = true)
    public DrivePresignedUrlResponseDto getPresignedDownloadUrl(UUID driveId, Integer expiresInSeconds) {
        DriveEntity drive = getOwnedDriveById(driveId);
        validateDownloadableDrive(drive);

        int effectiveExpiry = expiresInSeconds == null || expiresInSeconds <= 0
                ? DEFAULT_PRESIGNED_EXPIRY_SECONDS
                : expiresInSeconds;

        String url;
        try {
            url = minioService.generatePresignedDownloadUrl(drive.getMinioPath(), effectiveExpiry, resolveNotebookBucket());
        } catch (Exception exception) {
            throw new AppException(ApiResponseStatus.DRIVE_DOWNLOAD_FAILED);
        }
        return DrivePresignedUrlResponseDto.builder()
                .url(url)
                .expiresInSeconds(effectiveExpiry)
                .build();
    }

    @Transactional
    public DriveResponseDto updateFolder(UUID driveId, DriveUpdateFolderRequestDto requestDto) {
        DriveEntity folder = getOwnedDriveById(driveId);

        if (!folder.isFolder()) {
            throw new AppException(ApiResponseStatus.DRIVE_FOLDER_ONLY_OPERATION);
        }
        if (requestDto == null) {
            throw new AppException(ApiResponseStatus.DRIVE_FOLDER_UPDATE_REQUIRED);
        }

        boolean hasName = requestDto.getName() != null && !requestDto.getName().isBlank();
        boolean hasParent = requestDto.getParentId() != null;
        boolean moveToRoot = Boolean.TRUE.equals(requestDto.getMoveToRoot());

        if (!hasName && !hasParent && !moveToRoot) {
            throw new AppException(ApiResponseStatus.DRIVE_FOLDER_UPDATE_REQUIRED);
        }

        if (hasName) {
            folder.setName(requestDto.getName().trim());
        }

        if (moveToRoot) {
            folder.setParent(null);
        } else if (hasParent) {
            DriveEntity parent = getOwnedDriveById(requestDto.getParentId());
            if (!parent.isFolder()) {
                throw new AppException(ApiResponseStatus.DRIVE_PARENT_MUST_BE_FOLDER);
            }
            if (folder.getId().equals(parent.getId()) || isDescendantOf(parent, folder)) {
                throw new AppException(ApiResponseStatus.DRIVE_MOVE_CYCLE_NOT_ALLOWED);
            }
            folder.setParent(parent);
        }

        return driveMapper.entityToResponseDto(driveRepository.save(folder));
    }

    @Transactional
    public DriveResponseDto deleteById(UUID driveId) {
        DriveEntity drive = getOwnedDriveById(driveId);
        if (drive.isFolder()) {
            throw new AppException(ApiResponseStatus.DRIVE_FOLDER_ONLY_OPERATION);
        }

        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(drive))) {
            return driveMapper.entityToResponseDto(drive);
        }

        drive.setDeleteStatus(DataIngestionDeleteStatus.PENDING_DELETE);
        DriveResponseDto responseDto = driveMapper.entityToResponseDto(driveRepository.save(drive));
        executeDelete(drive);
        return responseDto;
    }

    @Transactional
    public void deleteFolderById(UUID driveId) {
        DriveEntity folder = getOwnedDriveById(driveId);
        if (!folder.isFolder()) {
            throw new AppException(ApiResponseStatus.DRIVE_FOLDER_ONLY_OPERATION);
        }

        deleteFolderRecursively(folder, folder.getOwner().getId(), folder.getOrganization().getId());
    }

    private void deleteFolderRecursively(DriveEntity folder, UUID ownerId, UUID orgId) {
        List<DriveEntity> children = driveRepository.findByParentIdAndOwnerIdAndOrganizationIdAndDeleteStatus(
                folder.getId(),
                ownerId,
                orgId,
                DataIngestionDeleteStatus.ACTIVE);

        for (DriveEntity child : children) {
            if (child.isFolder()) {
                deleteFolderRecursively(child, ownerId, orgId);
            } else {
                executeDelete(child);
            }
        }

        driveRepository.delete(folder);
    }

    private void executeDelete(DriveEntity drive) {
        try {
            if (drive.getMinioPath() != null && !drive.getMinioPath().isBlank()) {
                minioService.delete(drive.getMinioPath(), resolveNotebookBucket());
            }
            driveRepository.delete(drive);
        } catch (Exception exception) {
            driveRepository.findById(drive.getId()).ifPresent(entity -> {
                entity.setDeleteStatus(DataIngestionDeleteStatus.DELETE_FAILED);
                driveRepository.save(entity);
            });
            throw new AppException(ApiResponseStatus.DRIVE_DELETE_FAILED);
        }
    }

    private DriveEntity getOwnedDriveById(UUID driveId) {
        DriveEntity drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DRIVE_NOT_EXISTS));

        validateOwnership(drive, JwtUtil.getUserId(), JwtUtil.getOrgId());
        return drive;
    }

    private void validateOwnership(DriveEntity drive, UUID userId, UUID orgId) {
        if (drive.getOwner() == null || drive.getOrganization() == null) {
            throw new AppException(ApiResponseStatus.DRIVE_NOT_EXISTS);
        }
        if (!drive.getOwner().getId().equals(userId) || !drive.getOrganization().getId().equals(orgId)) {
            throw new AppException(ApiResponseStatus.DRIVE_NOT_EXISTS);
        }
    }

    private void validateDownloadableDrive(DriveEntity drive) {
        if (drive.isFolder()) {
            throw new AppException(ApiResponseStatus.DRIVE_FOLDER_ONLY_OPERATION);
        }
        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(drive))) {
            throw new AppException(ApiResponseStatus.DRIVE_DELETE_IN_PROGRESS);
        }
        if (drive.getMinioPath() == null || drive.getMinioPath().isBlank()) {
            throw new AppException(ApiResponseStatus.DRIVE_DOWNLOAD_FAILED);
        }
    }

    private DataIngestionDeleteStatus resolveDeleteStatus(DriveEntity drive) {
        return drive.getDeleteStatus() == null ? DataIngestionDeleteStatus.ACTIVE : drive.getDeleteStatus();
    }

    private boolean isDescendantOf(DriveEntity candidateParent, DriveEntity node) {
        DriveEntity cursor = candidateParent;
        while (cursor != null) {
            if (cursor.getId().equals(node.getId())) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    private DriveTreeResponseDto toTreeNode(DriveResponseDto responseDto) {
        DriveTreeResponseDto node = new DriveTreeResponseDto();
        node.setId(responseDto.getId());
        node.setName(responseDto.getName());
        node.setFolder(responseDto.isFolder());
        node.setContentType(responseDto.getContentType());
        node.setFileSize(responseDto.getFileSize());
        node.setMinioPath(responseDto.getMinioPath());
        node.setParentId(responseDto.getParentId());
        node.setOwnerId(responseDto.getOwnerId());
        node.setOrgId(responseDto.getOrgId());
        node.setDeleteStatus(responseDto.getDeleteStatus());
        node.setCreatedAt(responseDto.getCreatedAt());
        node.setCreatedBy(responseDto.getCreatedBy());
        node.setUpdatedAt(responseDto.getUpdatedAt());
        node.setUpdatedBy(responseDto.getUpdatedBy());
        return node;
    }

    private void sortTree(List<DriveTreeResponseDto> nodes) {
        nodes.sort(Comparator
                .comparing(DriveTreeResponseDto::isFolder)
                .reversed()
                .thenComparing(item -> item.getName() == null ? "" : item.getName().toLowerCase()));

        for (DriveTreeResponseDto node : nodes) {
            sortTree(node.getChildren());
        }
    }

    private String resolveNotebookBucket() {
        String configured = appProperties.getMinio() == null ? null : appProperties.getMinio().getNotebookBucket();
        if (configured == null || configured.isBlank()) {
            return DEFAULT_NOTEBOOK_BUCKET;
        }
        return configured;
    }
}
