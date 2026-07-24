package ai.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.AppProperties;
import ai.annotation.Audited;
import ai.dto.own.request.ShareLinkCreateRequestDto;
import ai.dto.own.request.filter.ShareLinkFilterDto;
import ai.dto.own.response.ShareLinkResponseDto;
import ai.dto.own.response.ShareLinkStatsDto;
import ai.dto.own.response.SharedResourceResponseDto;
import ai.dto.own.response.MessageResponseDto;
import ai.entity.postgres.NoteBookEntity;
import ai.entity.postgres.ShareLinkEntity;
import ai.entity.postgres.TopicEntity;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.enums.MessageParentType;
import ai.enums.ShareResource;
import ai.exception.AppException;
import ai.mapper.ShareLinkMapper;
import ai.model.CustomPairModel;
import ai.repository.ShareLinkRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý public share link cho Topic / Notebook.
 * <p>
 * Cơ chế bảo mật: token ngẫu nhiên (32 byte base62) + password tùy chọn (BCrypt) + expiry tùy chọn.
 * Viewer truy cập qua {@code /public/share/{token}} (read-only) — không cần JWT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShareLinkService {
    /** Số byte random tối thiểu cho token (an toàn). */
    static final int MIN_TOKEN_BYTES = 16;
    /** Số lần thử lại tối đa khi sinh token bị trùng. */
    static final int MAX_TOKEN_REGEN_ATTEMPTS = 10;

    ShareLinkRepository shareLinkRepository;
    ShareLinkMapper shareLinkMapper;
    TopicService topicService;
    NoteBookService noteBookService;
    MessageService messageService;
    TopicSourceService topicSourceService;
    NoteBookSourceService noteBookSourceService;
    UserService userService;
    AppProperties appProperties;
    org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    SecureRandom secureRandom = new SecureRandom();

    // ========================================================================
    // OWNER APIs
    // ========================================================================

    /**
     * Tạo share link mới. Owner phải là owner của resource (Topic/Notebook).
     */
    @Audited(action = AuditAction.SHARE, resource = AuditResource.SHARE_LINK, description = "Tạo link chia sẻ")
    @Transactional
    public ShareLinkResponseDto create(ShareLinkCreateRequestDto request) {
        UUID ownerId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        // Validate owner của resource
        validateResourceOwnership(request.getResourceType(), request.getResourceId(), ownerId);

        // Sinh token unique
        String token = generateUniqueToken();

        // Hash password nếu có
        String passwordHash = null;
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            passwordHash = passwordEncoder.encode(request.getPassword());
        }

        // Tính expiresAt
        Instant expiresAt = computeExpiresAt(request.getExpiryDays());

        // Lấy title snapshot của resource
        String resourceTitle = resolveResourceTitle(request.getResourceType(), request.getResourceId());

        ShareLinkEntity entity = ShareLinkEntity.builder()
                .token(token)
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .ownerId(ownerId)
                .organizationId(orgId)
                .title(request.getTitle())
                .passwordHash(passwordHash)
                .expiresAt(expiresAt)
                .build();

        ShareLinkEntity saved = shareLinkRepository.save(entity);
        return toResponseDto(saved, resourceTitle);
    }

    /**
     * List share link của owner hiện tại, có filter.
     */
    @Transactional(readOnly = true)
    public CustomPairModel<Long, List<ShareLinkResponseDto>> list(ShareLinkFilterDto filterDto) {
        UUID ownerId = JwtUtil.getUserId();
        Page<ShareLinkEntity> page = shareLinkRepository.findByOwnerWithFilter(
                ownerId,
                filterDto.getResourceType(),
                filterDto.getResourceId(),
                filterDto.createPageable());

        List<ShareLinkResponseDto> dtos = page.getContent().stream()
                .map(e -> toResponseDto(e, resolveResourceTitle(e.getResourceType(), e.getResourceId())))
                .toList();

        // Lọc activeOnly trong memory (đơn giản, tránh query phức tạp)
        if (Boolean.TRUE.equals(filterDto.getActiveOnly())) {
            List<ShareLinkResponseDto> filtered = dtos.stream().filter(ShareLinkResponseDto::isActive).toList();
            return new CustomPairModel<>((long) filtered.size(), filtered);
        }

        return new CustomPairModel<>(page.getTotalElements(), dtos);
    }

    /**
     * Revoke (hủy) share link. Chỉ owner mới được hủy.
     */
    @Audited(action = AuditAction.REVOKE, resource = AuditResource.SHARE_LINK, resourceIdExpression = "#arg0", description = "Hủy link chia sẻ")
    @Transactional
    public void revoke(UUID linkId) {
        UUID ownerId = JwtUtil.getUserId();
        ShareLinkEntity entity = shareLinkRepository.findById(linkId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.SHARE_LINK_NOT_EXISTS));

        if (!entity.getOwnerId().equals(ownerId)) {
            throw new AppException(ApiResponseStatus.SHARE_LINK_RESOURCE_OWNER_ONLY);
        }

        entity.setRevokedAt(Instant.now());
        shareLinkRepository.save(entity);
    }

    /**
     * Thống kê lượt xem của share link. Chỉ owner.
     */
    @Transactional(readOnly = true)
    public ShareLinkStatsDto getStats(UUID linkId) {
        UUID ownerId = JwtUtil.getUserId();
        ShareLinkEntity entity = shareLinkRepository.findById(linkId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.SHARE_LINK_NOT_EXISTS));

        if (!entity.getOwnerId().equals(ownerId)) {
            throw new AppException(ApiResponseStatus.SHARE_LINK_RESOURCE_OWNER_ONLY);
        }

        return ShareLinkStatsDto.builder()
                .viewCount(entity.getViewCount())
                .lastViewedAt(entity.getLastViewedAt())
                .active(!entity.isRevoked() && !entity.isExpired())
                .build();
    }

    // ========================================================================
    // PUBLIC VIEWER APIs
    // ========================================================================

    /**
     * Lấy entity share link theo token. Dùng cho filter.
     */
    @Transactional(readOnly = true)
    public ShareLinkEntity getByToken(String token) {
        return shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new AppException(ApiResponseStatus.SHARE_LINK_NOT_EXISTS));
    }

    /**
     * Validate access cho public viewer: kiểm tra revoked, expired, password.
     * Trả về entity nếu OK, throw AppException nếu không hợp lệ.
     */
    @Transactional(readOnly = true)
    public ShareLinkEntity validateAccess(String token, String providedPassword) {
        ShareLinkEntity entity = getByToken(token);

        if (entity.isRevoked()) {
            throw new AppException(ApiResponseStatus.SHARE_LINK_REVOKED);
        }
        if (entity.isExpired()) {
            throw new AppException(ApiResponseStatus.SHARE_LINK_EXPIRED);
        }
        if (entity.isPasswordRequired()) {
            if (providedPassword == null || providedPassword.isBlank()) {
                throw new AppException(ApiResponseStatus.SHARE_LINK_PASSWORD_REQUIRED);
            }
            if (!passwordEncoder.matches(providedPassword, entity.getPasswordHash())) {
                throw new AppException(ApiResponseStatus.SHARE_LINK_PASSWORD_INVALID);
            }
        }
        return entity;
    }

    /**
     * Tăng view count nguyên tử (async, không block response).
     */
    @Async
    @Transactional
    public void incrementViewCount(UUID linkId) {
        try {
            shareLinkRepository.incrementViewCount(linkId, Instant.now());
        } catch (Exception e) {
            log.warn("Failed to increment view count for share link {}: {}", linkId, e.getMessage());
        }
    }

    /**
     * Lấy metadata resource cho public viewer (read-only, không lộ ownerId/orgId).
     */
    @Transactional(readOnly = true)
    public SharedResourceResponseDto getSharedResource(ShareLinkEntity link) {
        return switch (link.getResourceType()) {
            case TOPIC -> {
                TopicEntity topic = topicService.getEntityByIdShared(link.getResourceId());
                UserEntity owner = userService.getEntityById(topic.getOwner().getId());
                yield SharedResourceResponseDto.builder()
                        .resourceType(ShareResource.TOPIC)
                        .resourceId(topic.getId())
                        .title(topic.getTitle())
                        .ownerDisplayName(buildDisplayName(owner))
                        .createdAt(topic.getAudit().getCreatedAt())
                        .messageCount(messageService.getAllShared(
                                topic.getId(), MessageParentType.TOPIC, emptyMessageFilter()).getFirst())
                        .sourceCount(topicSourceService.getSourcesShared(topic.getId(), 0, 1).getFirst())
                        .passwordRequired(link.isPasswordRequired())
                        .build();
            }
            case NOTEBOOK -> {
                NoteBookEntity notebook = noteBookService.getEntityByIdShared(link.getResourceId());
                UserEntity owner = userService.getEntityById(notebook.getOwner().getId());
                yield SharedResourceResponseDto.builder()
                        .resourceType(ShareResource.NOTEBOOK)
                        .resourceId(notebook.getId())
                        .title(notebook.getTitle())
                        .description(notebook.getDescription())
                        .instruction(notebook.getInstruction())
                        .ownerDisplayName(buildDisplayName(owner))
                        .createdAt(notebook.getAudit().getCreatedAt())
                        .messageCount(messageService.getAllShared(
                                notebook.getId(), MessageParentType.NOTEBOOK, emptyMessageFilter()).getFirst())
                        .sourceCount(noteBookSourceService.getSourcesShared(notebook.getId(), 0, 1).getFirst())
                        .passwordRequired(link.isPasswordRequired())
                        .build();
            }
        };
    }

    /**
     * Lấy messages cho public viewer (read-only).
     */
    @Transactional(readOnly = true)
    public CustomPairModel<Long, List<MessageResponseDto>> getSharedMessages(ShareLinkEntity link, int page, int size) {
        MessageParentType parentType = switch (link.getResourceType()) {
            case TOPIC -> MessageParentType.TOPIC;
            case NOTEBOOK -> MessageParentType.NOTEBOOK;
        };
        ai.dto.own.request.filter.MessageFilterDto filter = new ai.dto.own.request.filter.MessageFilterDto();
        filter.setPageNumber(page);
        filter.setPageSize(size);
        return messageService.getAllShared(link.getResourceId(), parentType, filter);
    }

    /**
     * Lấy sources cho public viewer (read-only).
     */
    @Transactional(readOnly = true)
    public Object getSharedSources(ShareLinkEntity link, int page, int size) {
        return switch (link.getResourceType()) {
            case TOPIC -> topicSourceService.getSourcesShared(link.getResourceId(), page, size);
            case NOTEBOOK -> noteBookSourceService.getSourcesShared(link.getResourceId(), page, size);
        };
    }

    /**
     * Lấy presigned download URL cho source (public viewer).
     */
    @Transactional(readOnly = true)
    public Object getSharedSourceDownloadUrl(ShareLinkEntity link, UUID sourceId, Integer expiresInSeconds) {
        return switch (link.getResourceType()) {
            case TOPIC -> topicSourceService.getSourceDownloadUrlShared(link.getResourceId(), sourceId, expiresInSeconds);
            case NOTEBOOK -> noteBookSourceService.getSourceDownloadUrlShared(link.getResourceId(), sourceId, expiresInSeconds);
        };
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private void validateResourceOwnership(ShareResource resourceType, UUID resourceId, UUID ownerId) {
        switch (resourceType) {
            case TOPIC -> topicService.validateTopicOfUser(resourceId, ownerId);
            case NOTEBOOK -> noteBookService.validateNoteBookOfUser(resourceId, ownerId);
        }
    }

    private String resolveResourceTitle(ShareResource resourceType, UUID resourceId) {
        try {
            return switch (resourceType) {
                case TOPIC -> topicService.getEntityByIdShared(resourceId).getTitle();
                case NOTEBOOK -> noteBookService.getEntityByIdShared(resourceId).getTitle();
            };
        } catch (Exception e) {
            log.warn("Could not resolve resource title for {} {}: {}", resourceType, resourceId, e.getMessage());
            return null;
        }
    }

    private String generateUniqueToken() {
        int tokenBytes = resolveTokenBytes();
        for (int i = 0; i < MAX_TOKEN_REGEN_ATTEMPTS; i++) {
            byte[] bytes = new byte[tokenBytes];
            secureRandom.nextBytes(bytes);
            // URL-safe base64, bỏ padding '='
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            if (!shareLinkRepository.existsByToken(token)) {
                return token;
            }
            log.warn("Share link token collision (attempt {}), regenerating...", i + 1);
        }
        throw new AppException(ApiResponseStatus.UNEXPECTED);
    }

    private int resolveTokenBytes() {
        Integer configured = appProperties.getShare() != null ? appProperties.getShare().getTokenLength() : null;
        if (configured == null || configured < MIN_TOKEN_BYTES) {
            return 32;
        }
        return configured;
    }

    private Instant computeExpiresAt(Integer expiryDays) {
        if (expiryDays == null) {
            // fallback default config
            Integer defaultDays = appProperties.getShare() != null ? appProperties.getShare().getDefaultExpiryDays() : null;
            if (defaultDays == null) {
                return null; // vĩnh viễn
            }
            expiryDays = defaultDays;
        }
        Integer maxDays = appProperties.getShare() != null && appProperties.getShare().getMaxExpiryDays() != null
                ? appProperties.getShare().getMaxExpiryDays()
                : 365;
        if (expiryDays != null && expiryDays > maxDays) {
            expiryDays = maxDays;
        }
        return Instant.now().plus(expiryDays, ChronoUnit.DAYS);
    }

    private ShareLinkResponseDto toResponseDto(ShareLinkEntity entity, String resourceTitle) {
        ShareLinkResponseDto dto = shareLinkMapper.entityToResponseDto(entity);
        dto.setResourceTitle(resourceTitle);
        dto.setUrl(buildShareUrl(entity.getToken()));
        return dto;
    }

    private String buildShareUrl(String token) {
        String baseUrl = appProperties.getShare() != null ? appProperties.getShare().getBaseUrl() : null;
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmed + "/" + token;
    }

    private String buildDisplayName(UserEntity user) {
        if (user == null) return null;
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isBlank() ? user.getUserName() : name;
    }

    private ai.dto.own.request.filter.MessageFilterDto emptyMessageFilter() {
        ai.dto.own.request.filter.MessageFilterDto filter = new ai.dto.own.request.filter.MessageFilterDto();
        filter.setPageNumber(0);
        filter.setPageSize(1);
        return filter;
    }
}