package ai.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ai.AppProperties;
import ai.dto.own.request.filter.AttachmentFilterDto;
import ai.dto.own.response.AttachmentDownloadData;
import ai.dto.own.response.AttachmentPresignedUrlResponseDto;
import ai.dto.own.response.AttachmentResponseDto;
import ai.entity.postgres.AttachmentEntity;
import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.TopicEntity;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.AttachmentMapper;
import ai.repository.AttachmentRepository;
import ai.util.JwtUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class AttachmentService {
    static String DEFAULT_ATTACHMENT_BUCKET = "attachments";
    static int DEFAULT_PRESIGNED_EXPIRY_SECONDS = 900;

    AttachmentRepository attachmentRepository;
    AttachmentMapper attachmentMapper;
    MinioService minioService;
    UserService userService;
    OrganizationService organizationService;
    TopicService topicService;
    MessageService messageService;
    AppProperties appProperties;

    @Transactional
    public AttachmentResponseDto uploadToTopic(UUID topicId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ApiResponseStatus.ATTACHMENT_FILE_REQUIRED);
        }

        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        UserEntity owner = userService.getEntityById(userId);
        OrganizationEntity organization = organizationService.getEntityById(orgId);
        topicService.validateTopicOfUser(topicId, userId);
        TopicEntity topic = topicService.getEntityById(topicId);

        String minioPath;
        try {
            minioPath = minioService.upload(
                    file,
                    owner.getUserName(),
                    organization.getName(),
                    resolveAttachmentBucket());
        } catch (Exception exception) {
            throw new AppException(ApiResponseStatus.ATTACHMENT_UPLOAD_FAILED);
        }

        AttachmentEntity attachment = new AttachmentEntity();
        attachment.setName(file.getOriginalFilename());
        attachment.setMinioPath(minioPath);
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setTopicId(topic.getId());
        attachment.setOwner(owner);
        attachment.setOrganization(organization);

        AttachmentResponseDto savedDto = attachmentMapper.entityToResponseDto(attachmentRepository.save(attachment));

        var attachmentMessage = messageService.createAttachmentMessage(topicId, savedDto);

        attachment = attachmentRepository.findById(savedDto.getId()).orElseThrow();
        attachment.setMessageId(attachmentMessage.getId());

        return attachmentMapper.entityToResponseDto(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public AttachmentResponseDto getDetails(UUID attachmentId) {
        AttachmentEntity attachment = getOwnedAttachmentById(attachmentId);
        return attachmentMapper.entityToResponseDto(attachment);
    }

    @Transactional(readOnly = true)
    public Page<AttachmentResponseDto> getList(AttachmentFilterDto filterDto) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        Specification<AttachmentEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) -> {
            Predicate orgPredicate = criteriaBuilder.equal(root.get("organization").get("id"), orgId);
            Predicate ownerPredicate = criteriaBuilder.equal(root.get("owner").get("id"), userId);
            return criteriaBuilder.and(orgPredicate, ownerPredicate);
        });

        return attachmentRepository.findAll(spec, filterDto.createPageable())
                .map(attachmentMapper::entityToResponseDto);
    }

    @Transactional(readOnly = true)
    public AttachmentDownloadData downloadById(UUID attachmentId) {
        AttachmentEntity attachment = getOwnedAttachmentById(attachmentId);

        if (attachment.getMinioPath() == null || attachment.getMinioPath().isBlank()) {
            throw new AppException(ApiResponseStatus.ATTACHMENT_DOWNLOAD_FAILED);
        }

        MinioService.MinioObjectData objectData;
        try {
            objectData = minioService.download(attachment.getMinioPath(), resolveAttachmentBucket());
        } catch (Exception exception) {
            throw new AppException(ApiResponseStatus.ATTACHMENT_DOWNLOAD_FAILED);
        }

        return new AttachmentDownloadData(
                attachment.getName(),
                objectData.getContentType(),
                objectData.getBytes());
    }

    @Transactional(readOnly = true)
    public AttachmentPresignedUrlResponseDto getPresignedDownloadUrl(UUID attachmentId, Integer expiresInSeconds) {
        AttachmentEntity attachment = getOwnedAttachmentById(attachmentId);

        if (attachment.getMinioPath() == null || attachment.getMinioPath().isBlank()) {
            throw new AppException(ApiResponseStatus.ATTACHMENT_DOWNLOAD_FAILED);
        }

        int effectiveExpiry = expiresInSeconds == null || expiresInSeconds <= 0
                ? DEFAULT_PRESIGNED_EXPIRY_SECONDS
                : expiresInSeconds;

        String url;
        try {
            url = minioService.generatePresignedDownloadUrl(
                    attachment.getMinioPath(),
                    effectiveExpiry,
                    resolveAttachmentBucket());
        } catch (Exception exception) {
            throw new AppException(ApiResponseStatus.ATTACHMENT_DOWNLOAD_FAILED);
        }

        return AttachmentPresignedUrlResponseDto.builder()
                .url(url)
                .expiresInSeconds(effectiveExpiry)
                .build();
    }

    private AttachmentEntity getOwnedAttachmentById(UUID attachmentId) {
        AttachmentEntity attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.ATTACHMENT_NOT_EXISTS));

        validateOwnership(attachment, JwtUtil.getUserId(), JwtUtil.getOrgId());
        return attachment;
    }

    private void validateOwnership(AttachmentEntity attachment, UUID userId, UUID orgId) {
        if (attachment.getOwner() == null || attachment.getOrganization() == null) {
            throw new AppException(ApiResponseStatus.ATTACHMENT_NOT_EXISTS);
        }

        if (!attachment.getOwner().getId().equals(userId)
                || !attachment.getOrganization().getId().equals(orgId)) {
            throw new AppException(ApiResponseStatus.ATTACHMENT_NOT_EXISTS);
        }
    }

    private String resolveAttachmentBucket() {
        String configured = appProperties.getMinio() == null ? null : appProperties.getMinio().getAttachmentBucket();
        if (configured == null || configured.isBlank()) {
            return DEFAULT_ATTACHMENT_BUCKET;
        }
        return configured;
    }
}
