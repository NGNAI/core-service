package ai.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.annotation.Audited;
import ai.dto.own.request.DraftCreateRequestDto;
import ai.dto.own.request.DraftSaveVersionRequestDto;
import ai.dto.own.response.DraftResponseDto;
import ai.dto.own.response.DraftVersionResponseDto;
import ai.entity.postgres.DraftEntity;
import ai.entity.postgres.DraftMessageEntity;
import ai.entity.postgres.DraftVersionEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.exception.AppException;
import ai.mapper.DraftMapper;
import ai.model.CustomPairModel;
import ai.repository.DraftMessagesRepository;
import ai.repository.DraftRepository;
import ai.repository.DraftVersionRepository;
import ai.repository.MessageFeedbackHistoryRepository;
import ai.service.api.RagApiService;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class DraftService {
    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    DraftRepository draftRepository;
    DraftVersionRepository draftVersionRepository;
    DraftMessagesRepository draftMessagesRepository;
    MessageFeedbackHistoryRepository messageFeedbackHistoryRepository;
    UserService userService;
    OrganizationService organizationService;
    RagApiService ragApiService;

    // @Autowired
    // @Lazy
    // @NonFinal
    // RagService ragService;
    
    DraftMapper draftMapper;

    public void validateDraftOfUser(UUID draftId, UUID userId) {
        if (!draftRepository.existsByIdAndOwnerId(draftId, userId)) {
            throw new AppException(ApiResponseStatus.PERMISSION_DENIED);
        }
    }

    public DraftEntity getEntityById(UUID draftId) {
        return draftRepository.findById(draftId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DRAFT_ID_NOT_EXISTS));
    }

    @Audited(action = AuditAction.CREATE, resource = AuditResource.DRAFT, description = "Tạo bản soạn thảo: {0}")
    @Transactional
    public DraftResponseDto create(DraftCreateRequestDto requestDto) {
        UUID userId = JwtUtil.getUserId();
        UUID organizationId = JwtUtil.getOrgId();

        DraftEntity entity = new DraftEntity();
        entity.setType(normalizeDraftType(requestDto.getType()));
        entity.setFormatStandard(normalizeFormatStandard(requestDto.getFormat()));
        entity.setTitle(normalizeRequired(requestDto.getTitle(), ApiResponseStatus.DRAFT_TITLE_CAN_NOT_BE_NULL_OR_EMPTY));
        entity.setDetailedDescription(normalizeRequired(requestDto.getDetailedDescription(), ApiResponseStatus.DRAFT_DESCRIPTION_CAN_NOT_BE_NULL_OR_EMPTY));
        entity.setLatestVersionNumber(0);
        entity.setOwner(userService.getEntityById(userId));
        entity.setOrganization(organizationService.getEntityById(organizationId));

        return draftMapper.entityToResponseDto(draftRepository.save(entity));
    }

    @Audited(action = AuditAction.DELETE, resource = AuditResource.DRAFT, resourceIdExpression = "#arg0", description = "Xoá bản soạn thảo: {0}")
    @Transactional
    public void delete(UUID draftId) {
        UUID userId = JwtUtil.getUserId();
        validateDraftOfUser(draftId, userId);

        // 1. Xoá feedback history của các message thuộc draft trước (FK message_id)
        List<DraftMessageEntity> draftMessages = draftMessagesRepository.findByDraft_IdOrderById_MessageIdAsc(draftId);
        if (!draftMessages.isEmpty()) {
            List<UUID> messageIds = draftMessages.stream()
                    .map(draftMessage -> draftMessage.getMessageEntity().getId())
                    .toList();
            messageFeedbackHistoryRepository.deleteByMessage_IdIn(messageIds);
        }

        // 2. Xoá draft_messages (cascade xoá message entity qua CascadeType.ALL)
        draftMessagesRepository.deleteAll(draftMessages);

        // 3. Xoá các version của draft
        draftVersionRepository.deleteByDraft_Id(draftId);

        // 4. Xoá draft
        draftRepository.deleteById(draftId);
    }
   
    @Transactional
    @Audited(action = AuditAction.SAVE_VERSION, resource = AuditResource.DRAFT_VERSION, resourceIdExpression = "#arg0", description = "Lưu phiên bản mới cho draft: {0}")
    public DraftVersionResponseDto saveVersion(UUID draftId, DraftSaveVersionRequestDto requestDto) {
        DraftEntity draft = getEntityById(draftId);

        String resolvedDescription = resolveDetailedDescription(draft.getDetailedDescription(), requestDto.getDetailedDescription());
        draft.setDetailedDescription(resolvedDescription);

        Integer currentVersion = draft.getLatestVersionNumber();
        int nextVersion = Objects.requireNonNullElse(currentVersion, 0) + 1;

        String generatedContent = normalizeRequired(
                requestDto.getCurrentDraftContent(),
                ApiResponseStatus.DRAFT_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY);

        DraftVersionEntity savedVersion = draftVersionRepository.save(
                buildVersion(
                        draft,
                        nextVersion,
                        resolvedDescription,
                        requestDto.getChangeRequest(),
                        generatedContent));

        draft.setLatestVersionNumber(nextVersion);
        draft.setLatestContentPreview(buildContentPreview(generatedContent));
        draftRepository.save(draft);

        return draftMapper.versionEntityToResponseDto(savedVersion);
    }

    @Audited(action = AuditAction.DELETE, resource = AuditResource.DRAFT_VERSION, resourceIdExpression = "#arg0", description = "Xoá phiên bản: {0}")
    @Transactional
    public void deleteVersion(UUID draftId, UUID versionId) {
        UUID userId = JwtUtil.getUserId();
        validateDraftOfUser(draftId, userId);

        DraftEntity draft = getEntityById(draftId);
        DraftVersionEntity versionEntity = getDraftVersionEntity(draftId, versionId);

        if (draftVersionRepository.countByDraft_Id(draftId) <= 1) {
            throw new AppException(ApiResponseStatus.DRAFT_LAST_VERSION_CAN_NOT_BE_DELETED);
        }

        draftVersionRepository.delete(versionEntity);

        syncLatestVersionMetadata(draft);
    }

    @Audited(action = AuditAction.ROLLBACK, resource = AuditResource.DRAFT, resourceIdExpression = "#arg0", description = "Khôi phục phiên bản draft: {0}")
    @Transactional
    public DraftVersionResponseDto rollbackToVersion(UUID draftId, UUID versionId, String rollbackReason) {
        UUID userId = JwtUtil.getUserId();
        validateDraftOfUser(draftId, userId);

        DraftEntity draft = getEntityById(draftId);
        DraftVersionEntity targetVersion = getDraftVersionEntity(draftId, versionId);

        String normalizedRollbackReason = normalizeRequired(
                rollbackReason,
                ApiResponseStatus.DRAFT_ROLLBACK_REASON_CAN_NOT_BE_NULL_OR_EMPTY);

        Integer currentVersion = draft.getLatestVersionNumber();
        int nextVersion = java.util.Objects.requireNonNullElse(currentVersion, 0) + 1;

        DraftVersionEntity rollbackVersion = buildVersion(
                draft,
                nextVersion,
                targetVersion.getDetailedDescription(),
                normalizedRollbackReason,
                targetVersion.getGeneratedContent());

        DraftVersionEntity savedRollbackVersion = draftVersionRepository.save(rollbackVersion);

        // Sau khi rollback, cập nhật metadata của draft về phiên bản mới nhất
        draft.setDetailedDescription(targetVersion.getDetailedDescription());
        draft.setLatestVersionNumber(nextVersion);
        draft.setLatestContentPreview(buildContentPreview(targetVersion.getGeneratedContent()));
        draftRepository.save(draft);

        return draftMapper.versionEntityToResponseDto(savedRollbackVersion);
    }

    @Transactional(readOnly = true)
    public DraftResponseDto getById(UUID draftId) {
        UUID userId = JwtUtil.getUserId();
        validateDraftOfUser(draftId, userId);
        return draftMapper.entityToResponseDto(getEntityById(draftId));
    }

    @Transactional(readOnly = true)
    public CustomPairModel<Long, List<DraftResponseDto>> getAll(int pageNumber, int pageSize) {
        UUID userId = JwtUtil.getUserId();
        UUID organizationId = JwtUtil.getOrgId();

        int normalizedPageNumber = Math.max(pageNumber, 0);
        int normalizedPageSize = normalizePageSize(pageSize);

        Page<DraftEntity> page = draftRepository.findByOwner_IdAndOrganization_IdOrderByAudit_UpdatedAtDesc(
                userId,
                organizationId,
                PageRequest.of(normalizedPageNumber, normalizedPageSize));

        List<DraftResponseDto> data = page.getContent().stream()
                .map(draftMapper::entityToResponseDto)
                .toList();

        return new CustomPairModel<>(page.getTotalElements(), data);
    }

    @Transactional(readOnly = true)
    public CustomPairModel<Long, List<DraftVersionResponseDto>> getVersions(UUID draftId, int pageNumber, int pageSize) {
        UUID userId = JwtUtil.getUserId();
        validateDraftOfUser(draftId, userId);

        int normalizedPageNumber = Math.max(pageNumber, 0);
        int normalizedPageSize = normalizePageSize(pageSize);

        Page<DraftVersionEntity> page = draftVersionRepository.findByDraft_IdOrderByVersionNumberDesc(
                draftId,
                PageRequest.of(normalizedPageNumber, normalizedPageSize));

        List<DraftVersionResponseDto> data = page.getContent().stream()
                .map(draftMapper::versionEntityToResponseDto)
                .toList();

        return new CustomPairModel<>(page.getTotalElements(), data);
    }

    @Transactional(readOnly = true)
    public String getLatestContentOfCurrentUser(UUID draftId) {
        UUID userId = JwtUtil.getUserId();
        validateDraftOfUser(draftId, userId);

        return draftVersionRepository.findFirstByDraft_IdOrderByVersionNumberDesc(draftId)
                .map(DraftVersionEntity::getGeneratedContent)
                .orElse(null);
    }

    private DraftVersionEntity getDraftVersionEntity(UUID draftId, UUID versionId) {
        return draftVersionRepository.findByIdAndDraft_Id(versionId, draftId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DRAFT_VERSION_NOT_EXISTS));
    }

    /**
     * Đồng bộ metadata của draft với phiên bản mới nhất sau khi có sự thay đổi về phiên bản (xóa hoặc rollback)
     * @param draft
     */
    private void syncLatestVersionMetadata(DraftEntity draft) {
        draftVersionRepository.findFirstByDraft_IdOrderByVersionNumberDesc(draft.getId())
                .ifPresentOrElse(
                        latestVersion -> {
                            draft.setDetailedDescription(latestVersion.getDetailedDescription());
                            draft.setLatestVersionNumber(latestVersion.getVersionNumber());
                            draft.setLatestContentPreview(buildContentPreview(latestVersion.getGeneratedContent()));
                        },
                        () -> {
                            draft.setLatestVersionNumber(0);
                            draft.setLatestContentPreview(null);
                        });

        draftRepository.save(draft);
    }


    /**
     * Xây dựng DraftVersionEntity từ các thông tin đầu vào. Hàm này sẽ không set id vì id sẽ được tự động sinh bởi database khi lưu entity.
     * @param draft
     * @param versionNumber
     * @param detailedDescription
     * @param changeRequest
     * @param generatedContent
     * @return
     */
    private DraftVersionEntity buildVersion(
            DraftEntity draft,
            int versionNumber,
            String detailedDescription,
            String changeRequest,
            String generatedContent) {
        DraftVersionEntity versionEntity = new DraftVersionEntity();
        versionEntity.setDraft(draft);
        versionEntity.setVersionNumber(versionNumber);
        versionEntity.setDetailedDescription(detailedDescription);
        versionEntity.setChangeRequest(normalizeNullable(changeRequest));
        versionEntity.setGeneratedContent(generatedContent);
        return versionEntity;
    }

    /**
     * Chuẩn hóa + validate type: bắt buộc phải nằm trong danh sách document types từ RAG service.
     */
    private String normalizeDraftType(String type) {
        String normalized = normalizeRequired(type, ApiResponseStatus.DRAFT_TYPE_CAN_NOT_BE_NULL_OR_EMPTY)
                .toLowerCase(Locale.ROOT);

        boolean valid = ragApiService.getDraftDocumentTypes().stream()
                .anyMatch(doc -> normalized.equalsIgnoreCase(doc.getValue()));
        if (!valid) {
            throw new AppException(ApiResponseStatus.INVALID_DRAFT_TYPE_VALUE);
        }

        return normalized;
    }

    /**
     * Chuẩn hóa + validate format (format_standard): là field tùy chọn nên null/rỗng đều hợp lệ.
     * Nếu có giá trị, bắt buộc phải nằm trong danh sách format standards từ RAG service.
     */
    private String normalizeFormatStandard(String format) {
        String normalized = normalizeNullable(format);
        if (normalized == null) {
            return null;
        }
        String lowerCased = normalized.toLowerCase(Locale.ROOT);

        boolean valid = ragApiService.getDraftFormatStandards().stream()
                .anyMatch(std -> lowerCased.equalsIgnoreCase(std.getId()));
        if (!valid) {
            throw new AppException(ApiResponseStatus.INVALID_DRAFT_FORMAT_STANDARD_VALUE);
        }

        return lowerCased;
    }

    private String resolveDetailedDescription(String existingValue, String updateValue) {
        String normalizedUpdate = normalizeNullable(updateValue);
        if (normalizedUpdate != null) {
            return normalizedUpdate;
        }

        String normalizedExisting = normalizeNullable(existingValue);
        if (normalizedExisting == null) {
            throw new AppException(ApiResponseStatus.DRAFT_DESCRIPTION_CAN_NOT_BE_NULL_OR_EMPTY);
        }

        return normalizedExisting;
    }

    /**
     * Trả về toàn bộ nội dung để lưu vào latestContentPreview.
     * Cột đã là TEXT nên có thể lưu nội dung soạn thảo dài (vài trang A4), không cắt ngắn.
     */
    private String buildContentPreview(String content) {
        return normalizeNullable(content);
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String normalizeRequired(String value, ApiResponseStatus statusWhenBlank) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new AppException(statusWhenBlank);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Audited(action = AuditAction.UPDATE, resource = AuditResource.DRAFT, resourceIdExpression = "#arg0", description = "Cập nhật sessionId cho draft: {0}")
    @Transactional
    public void updateSessionId(UUID id, String sessionIdStr) {
        DraftEntity draft = getEntityById(id);
        draft.setSessionId(sessionIdStr);
        draftRepository.save(draft);
    }
   
}
