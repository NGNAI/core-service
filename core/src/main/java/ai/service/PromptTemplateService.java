package ai.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import ai.annotation.Audited;
import ai.dto.own.request.PromptTemplateCreateRequestDto;
import ai.dto.own.request.PromptTemplateUpdateRequestDto;
import ai.dto.own.request.filter.PromptTemplateFilterDto;
import ai.dto.own.response.PromptTemplateResponseDto;
import ai.entity.postgres.PromptTemplateEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.enums.PromptScope;
import ai.exception.AppException;
import ai.mapper.PromptTemplateMapper;
import ai.model.CustomPairModel;
import ai.repository.PromptTemplateRepository;
import ai.util.JwtUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Service quản lý Quick Prompt Template cho chat với Topic / NotebookLM.
 * <p>
 * Hai luồng sử dụng:
 * <ul>
 *   <li><b>USER flow</b> — user CRUD prompt cá nhân (scope=USER) + xem system prompt (scope=SYSTEM, global).</li>
 *   <li><b>ADMIN flow</b> — admin CRUD system prompt (global) + xem/sửa/xóa tất cả user prompt trong org của admin.</li>
 * </ul>
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class PromptTemplateService {

    PromptTemplateRepository promptTemplateRepository;
    PromptTemplateMapper promptTemplateMapper;
    UserService userService;
    OrganizationService organizationService;

    // ========================================================================
    // Validations & helpers
    // ========================================================================

    /**
     * Kiểm tra prompt thuộc về user hiện tại (dùng khi user sửa/xóa prompt của mình).
     */
    public void validatePromptOfUser(UUID promptId, UUID userId) {
        if (!promptTemplateRepository.existsByIdAndOwnerId(promptId, userId))
            throw new AppException(ApiResponseStatus.PROMPT_TEMPLATE_OWNER_ONLY);
    }

    /**
     * Lấy entity prompt theo id (không check quyền), throw nếu không tồn tại.
     */
    public PromptTemplateEntity getEntityById(UUID promptId) {
        return promptTemplateRepository.findById(promptId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.PROMPT_TEMPLATE_NOT_EXISTS));
    }

    /**
     * Admin chỉ được truy cập system prompt (global) hoặc user prompt thuộc org của admin.
     */
    private void validateAdminCanAccess(PromptTemplateEntity entity, UUID orgId) {
        boolean isSystem = entity.getScope() == PromptScope.SYSTEM;
        boolean isInOrg = entity.getOrganization() != null && orgId.equals(entity.getOrganization().getId());
        if (!isSystem && !isInOrg)
            throw new AppException(ApiResponseStatus.PROMPT_TEMPLATE_OWNER_ONLY);
    }

    // ========================================================================
    // USER flow
    // ========================================================================

    /**
     * List prompt cho user: system prompt (active, global) + user prompt của chính mình (active, trong org hiện tại).
     * Lọc thêm theo {@code promptType}, {@code scope}, {@code keyword} nếu có.
     */
    public CustomPairModel<Long, List<PromptTemplateResponseDto>> getAllForUser(PromptTemplateFilterDto filterDto) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        userService.validateUserId(userId);
        organizationService.validateOrgId(orgId);

        Specification<PromptTemplateEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) -> {
            Predicate systemActive = criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("scope"), PromptScope.SYSTEM),
                    criteriaBuilder.isTrue(root.get("isActive")));
            Predicate ownActive = criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("scope"), PromptScope.USER),
                    criteriaBuilder.equal(root.get("owner").get("id"), userId),
                    criteriaBuilder.equal(root.get("organization").get("id"), orgId),
                    criteriaBuilder.isTrue(root.get("isActive")));
            return criteriaBuilder.or(systemActive, ownActive);
        });

        Page<PromptTemplateEntity> page = promptTemplateRepository.findAll(spec, filterDto.createPageable());

        return new CustomPairModel<>(page.getTotalElements(),
                page.getContent().stream().map(promptTemplateMapper::entityToResponseDto).toList());
    }

    /**
     * Xem chi tiết prompt: user chỉ xem được system prompt hoặc prompt của chính mình.
     */
    public PromptTemplateResponseDto getByIdForUser(UUID promptId) {
        UUID userId = JwtUtil.getUserId();
        PromptTemplateEntity entity = getEntityById(promptId);

        boolean isSystem = entity.getScope() == PromptScope.SYSTEM;
        boolean isOwn = entity.getOwner() != null && userId.equals(entity.getOwner().getId());
        if (!isSystem && !isOwn)
            throw new AppException(ApiResponseStatus.PROMPT_TEMPLATE_OWNER_ONLY);

        return promptTemplateMapper.entityToResponseDto(entity);
    }

    /**
     * User tạo prompt cá nhân (scope=USER, gắn owner + org từ JWT).
     */
    @Audited(action = AuditAction.CREATE, resource = AuditResource.PROMPT_TEMPLATE, description = "Tạo prompt template: {0}")
    public PromptTemplateResponseDto create(PromptTemplateCreateRequestDto requestDto) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        PromptTemplateEntity entity = promptTemplateMapper.createRequestDtoToEntity(requestDto);
        entity.setScope(PromptScope.USER);
        entity.setOwner(userService.getEntityById(userId));
        entity.setOrganization(organizationService.getEntityById(orgId));

        return promptTemplateMapper.entityToResponseDto(promptTemplateRepository.save(entity));
    }

    /**
     * User cập nhật prompt cá nhân của mình (partial update).
     */
    @Audited(action = AuditAction.UPDATE, resource = AuditResource.PROMPT_TEMPLATE, resourceIdExpression = "#arg0", description = "Cập nhật prompt template: {0}")
    public PromptTemplateResponseDto update(UUID promptId, PromptTemplateUpdateRequestDto requestDto) {
        validatePromptOfUser(promptId, JwtUtil.getUserId());
        PromptTemplateEntity entity = getEntityById(promptId);
        promptTemplateMapper.updateEntity(entity, requestDto);

        return promptTemplateMapper.entityToResponseDto(promptTemplateRepository.save(entity));
    }

    /**
     * User xóa prompt cá nhân của mình.
     */
    @Audited(action = AuditAction.DELETE, resource = AuditResource.PROMPT_TEMPLATE, resourceIdExpression = "#arg0", description = "Xoá prompt template: {0}")
    public void delete(UUID promptId) {
        validatePromptOfUser(promptId, JwtUtil.getUserId());
        promptTemplateRepository.deleteById(promptId);
    }

    // ========================================================================
    // ADMIN flow
    // ========================================================================

    /**
     * List tất cả prompt cho admin: system prompt (global) + user prompt trong org của admin.
     * Có thể lọc theo {@code scope} (SYSTEM/USER), {@code promptType}, {@code isActive}, {@code keyword}.
     */
    public CustomPairModel<Long, List<PromptTemplateResponseDto>> getAllForAdmin(PromptTemplateFilterDto filterDto) {
        UUID orgId = JwtUtil.getOrgId();
        organizationService.validateOrgId(orgId);

        Specification<PromptTemplateEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) -> {
            Predicate systemPred = criteriaBuilder.equal(root.get("scope"), PromptScope.SYSTEM);
            Predicate userInOrgPred = criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("scope"), PromptScope.USER),
                    criteriaBuilder.equal(root.get("organization").get("id"), orgId));
            return criteriaBuilder.or(systemPred, userInOrgPred);
        });

        Page<PromptTemplateEntity> page = promptTemplateRepository.findAll(spec, filterDto.createPageable());

        return new CustomPairModel<>(page.getTotalElements(),
                page.getContent().stream().map(promptTemplateMapper::entityToResponseDto).toList());
    }

    /**
     * Xem chi tiết prompt cho admin (system hoặc user prompt trong org của admin).
     */
    public PromptTemplateResponseDto getByIdForAdmin(UUID promptId) {
        PromptTemplateEntity entity = getEntityById(promptId);
        validateAdminCanAccess(entity, JwtUtil.getOrgId());
        return promptTemplateMapper.entityToResponseDto(entity);
    }

    /**
     * Admin tạo system prompt (scope=SYSTEM, global — không gắn owner/org).
     */
    @Audited(action = AuditAction.CREATE, resource = AuditResource.PROMPT_TEMPLATE, description = "Tạo system prompt template: {0}")
    public PromptTemplateResponseDto createSystem(PromptTemplateCreateRequestDto requestDto) {
        PromptTemplateEntity entity = promptTemplateMapper.createRequestDtoToEntity(requestDto);
        entity.setScope(PromptScope.SYSTEM);
        entity.setOwner(null);
        entity.setOrganization(null);

        return promptTemplateMapper.entityToResponseDto(promptTemplateRepository.save(entity));
    }

    /**
     * Admin cập nhật prompt bất kỳ (system hoặc user prompt trong org của admin).
     */
    @Audited(action = AuditAction.UPDATE, resource = AuditResource.PROMPT_TEMPLATE, resourceIdExpression = "#arg0", description = "Cập nhật prompt template: {0}")
    public PromptTemplateResponseDto updateForAdmin(UUID promptId, PromptTemplateUpdateRequestDto requestDto) {
        PromptTemplateEntity entity = getEntityById(promptId);
        validateAdminCanAccess(entity, JwtUtil.getOrgId());
        promptTemplateMapper.updateEntity(entity, requestDto);

        return promptTemplateMapper.entityToResponseDto(promptTemplateRepository.save(entity));
    }

    /**
     * Admin xóa prompt bất kỳ (system hoặc user prompt trong org của admin).
     */
    @Audited(action = AuditAction.DELETE, resource = AuditResource.PROMPT_TEMPLATE, resourceIdExpression = "#arg0", description = "Xoá prompt template: {0}")
    public void deleteForAdmin(UUID promptId) {
        PromptTemplateEntity entity = getEntityById(promptId);
        validateAdminCanAccess(entity, JwtUtil.getOrgId());
        promptTemplateRepository.deleteById(promptId);
    }
}
