package ai.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.annotation.Audited;
import ai.dto.own.request.FeedbackCreateRequestDto;
import ai.dto.own.request.FeedbackRespondRequestDto;
import ai.dto.own.request.FeedbackStatusUpdateRequestDto;
import ai.dto.own.request.FeedbackUpdateRequestDto;
import ai.dto.own.request.filter.FeedbackFilterDto;
import ai.dto.own.response.FeedbackResponseDto;
import ai.entity.postgres.FeedbackEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.exception.AppException;
import ai.mapper.FeedbackMapper;
import ai.model.CustomPairModel;
import ai.repository.FeedbackRepository;
import ai.util.JwtUtil;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class FeedbackService {
    FeedbackRepository feedbackRepository;
    UserService userService;
    OrganizationService organizationService;
    PermissionCheckerService permissionCheckerService;
    FeedbackMapper feedbackMapper;

    // Validation methods
    public void validateFeedbackOfUser(UUID feedbackId, UUID userId) {
        if (!feedbackRepository.existsByIdAndSenderId(feedbackId, userId)) {
            throw new AppException(ApiResponseStatus.PERMISSION_DENIED);
        }
    }

    public void validateFeedbackId(UUID feedbackId) {
        if (!feedbackRepository.existsById(feedbackId)) {
            throw new AppException(ApiResponseStatus.FEEDBACK_ID_NOT_EXISTS);
        }
    }

    /**
     * Lấy feedback theo id <b>không kiểm tra ownership</b>.
     * Dùng cho admin flow.
     */
    public FeedbackEntity getEntityByIdShared(UUID feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.FEEDBACK_ID_NOT_EXISTS));
    }

    public FeedbackMapper getFeedbackMapper() {
        return feedbackMapper;
    }

    // Admin queries
    public CustomPairModel<Long, List<FeedbackResponseDto>> getAllFeedbacks(FeedbackFilterDto filterDto) {
        UUID userId = JwtUtil.getUserId();
        userService.validateUserId(userId);

        // Global admin (có quyền DASHBOARD_GLOBAL) xem tất cả feedback,
        // admin thường chỉ xem feedback của org mình
        boolean isGlobalAdmin = permissionCheckerService.canAccess(null, "DASHBOARD_GLOBAL", "READ", null);

        Specification<FeedbackEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) -> {
            if (isGlobalAdmin) {
                return null; // global admin xem tất cả orgs
            }
            UUID orgId = JwtUtil.getOrgId();
            return criteriaBuilder.equal(root.get("senderOrg").get("id"), orgId);
        });

        Page<FeedbackEntity> page = feedbackRepository.findAll(
                spec,
                filterDto.createPageable()
        );

        List<FeedbackResponseDto> list = page.map(feedbackMapper::entityToResponseDto).getContent();
        return new CustomPairModel<>(page.getTotalElements(), list);
    }

    // User flows
    public CustomPairModel<Long, List<FeedbackResponseDto>> getAllForCurrentUser(FeedbackFilterDto filterDto) {
        UUID userId = JwtUtil.getUserId();
        userService.validateUserId(userId);

        Specification<FeedbackEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("sender").get("id"), userId));

        Page<FeedbackEntity> page = feedbackRepository.findAll(spec, filterDto.createPageable());
        List<FeedbackResponseDto> list = page.map(feedbackMapper::entityToResponseDto).getContent();
        return new CustomPairModel<>(page.getTotalElements(), list);
    }

    public FeedbackResponseDto getFeedbackOfUser(UUID feedbackId) {
        UUID userId = JwtUtil.getUserId();
        validateFeedbackOfUser(feedbackId, userId);
        return feedbackMapper.entityToResponseDto(getEntityByIdShared(feedbackId));
    }

    @Counted(value = "api.feedback.create.calls", description = "Số lần gọi create Feedback")
    @Timed(value = "api.feedback.create", description = "Thời gian create Feedback")
    @Audited(action = AuditAction.CREATE, resource = AuditResource.FEEDBACK, description = "Tạo góp ý: {0}")
    public FeedbackResponseDto createFeedback(FeedbackCreateRequestDto dto) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        userService.validateUserId(userId);
        organizationService.validateOrgId(orgId);

        FeedbackEntity newEntity = new FeedbackEntity();
        newEntity.setSubject(dto.getSubject());
        newEntity.setContent(dto.getContent());
        newEntity.setIsPrivate(Boolean.TRUE.equals(dto.getIsPrivate()));
        newEntity.setStatus(ai.enums.FeedbackStatus.PENDING);
        newEntity.setSender(userService.getEntityById(userId));
        newEntity.setSenderOrg(organizationService.getEntityById(orgId));

        return feedbackMapper.entityToResponseDto(feedbackRepository.save(newEntity));
    }

    @Audited(action = AuditAction.UPDATE, resource = AuditResource.FEEDBACK, resourceIdExpression = "#arg0", description = "Cập nhật góp ý: {0}")
    public FeedbackResponseDto updateFeedback(UUID id, FeedbackUpdateRequestDto dto) {
        UUID userId = JwtUtil.getUserId();
        validateFeedbackOfUser(id, userId);

        FeedbackEntity entity = feedbackRepository.findById(id)
                .orElseThrow(() -> new AppException(ApiResponseStatus.FEEDBACK_ID_NOT_EXISTS));

        if (dto.getSubject() != null) {
            entity.setSubject(dto.getSubject());
        }
        if (dto.getContent() != null) {
            entity.setContent(dto.getContent());
        }
        if (dto.getIsPrivate() != null) {
            entity.setIsPrivate(dto.getIsPrivate());
        }

        return feedbackMapper.entityToResponseDto(feedbackRepository.save(entity));
    }

    @Transactional
    @Audited(action = AuditAction.DELETE, resource = AuditResource.FEEDBACK, resourceIdExpression = "#arg0", description = "Xoá góp ý: {0}")
    public void deleteFeedback(UUID id) {
        UUID userId = JwtUtil.getUserId();
        validateFeedbackOfUser(id, userId);

        feedbackRepository.deleteById(id);
    }

    @Transactional
    @Audited(action = AuditAction.DELETE, resource = AuditResource.FEEDBACK, resourceIdExpression = "#arg0", description = "Admin xoá góp ý: {0}")
    public void deleteFeedbackShared(UUID id) {
        validateFeedbackId(id);
        feedbackRepository.deleteById(id);
    }

    // Admin flows
    @Audited(action = AuditAction.UPDATE, resource = AuditResource.FEEDBACK, resourceIdExpression = "#arg0", description = "Phản hồi góp ý: {0}")
    public FeedbackResponseDto respondFeedback(UUID id, FeedbackRespondRequestDto dto) {
        UUID adminId = JwtUtil.getUserId();
        UUID adminOrgId = JwtUtil.getOrgId();

        userService.validateUserId(adminId);
        organizationService.validateOrgId(adminOrgId);

        FeedbackEntity entity = getEntityByIdShared(id);

        if (entity.getResponseDate() != null) {
            throw new AppException(ApiResponseStatus.FEEDBACK_ALREADY_RESPONDED);
        }

        entity.setResponseContent(dto.getResponseContent());
        entity.setResponseDate(Instant.now());
        entity.setResponder(userService.getEntityById(adminId));
        entity.setResponderOrg(organizationService.getEntityById(adminOrgId));
        entity.setStatus(ai.enums.FeedbackStatus.RESOLVED);

        return feedbackMapper.entityToResponseDto(feedbackRepository.save(entity));
    }

    @Audited(action = AuditAction.UPDATE, resource = AuditResource.FEEDBACK, resourceIdExpression = "#arg0", description = "Cập nhật trạng thái góp ý: {0}")
    public FeedbackResponseDto updateStatus(UUID id, FeedbackStatusUpdateRequestDto dto) {
        UUID adminId = JwtUtil.getUserId();

        userService.validateUserId(adminId);

        FeedbackEntity entity = getEntityByIdShared(id);
        entity.setStatus(dto.getStatus());

        return feedbackMapper.entityToResponseDto(feedbackRepository.save(entity));
    }
}
