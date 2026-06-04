package ai.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ai.dto.own.request.audit.AuditLogRequest;
import ai.dto.own.request.filter.AuditLogFilterDto;
import ai.dto.own.response.AuditLogResponseDto;
import ai.entity.postgres.AuditLogEntity;
import ai.entity.postgres.OrganizationEntity;
import ai.enums.AuditStatus;
import ai.mapper.AuditLogMapper;
import ai.model.CustomPairModel;
import ai.repository.AuditLogRepository;
import ai.repository.OrganizationRepository;
import ai.util.JwtUtil;
import ai.util.RequestContextUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditLogService {

    AuditLogRepository auditLogRepository;
    AuditLogMapper auditLogMapper;
    RequestContextUtil requestContextUtil;
    OrganizationRepository organizationRepository;

    /**
     * Record an audit log entry. Persists asynchronously and in a new transaction
     * so that the failure of the underlying business transaction does not roll back
     * the audit entry and vice versa.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLogRequest request) {
        try {
            AuditLogEntity.AuditLogEntityBuilder builder = AuditLogEntity.builder()
                    .action(request.getAction())
                    .resource(request.getResource())
                    .resourceId(truncate(request.getResourceId(), 128))
                    .resourceName(truncate(request.getResourceName(), 512))
                    .description(truncate(request.getDescription(), 1024))
                    .userId(request.getUserId() != null ? request.getUserId() : JwtUtil.getUserId())
                    .userName(request.getUserName() != null
                            ? request.getUserName()
                            : requestContextUtil.userName(
                                    request.getUserId() != null ? request.getUserId() : JwtUtil.getUserId()))
                    .orgId(request.getOrgId() != null ? request.getOrgId() : JwtUtil.getOrgId())
                    .organizationName(request.getOrganizationName() != null
                            ? request.getOrganizationName()
                            : resolveOrgName(request.getOrgId() != null ? request.getOrgId() : JwtUtil.getOrgId()))
                    .ipAddress(request.getIpAddress() != null ? request.getIpAddress() : requestContextUtil.clientIp())
                    .userAgent(request.getUserAgent() != null ? request.getUserAgent() : requestContextUtil.userAgent())
                    .method(request.getMethod() != null ? request.getMethod() : requestContextUtil.method())
                    .path(request.getPath() != null ? request.getPath() : requestContextUtil.path())
                    .status(request.isSuccess() ? AuditStatus.SUCCESS : AuditStatus.FAILED)
                    .details(request.getDetails())
                    .errorMessage(truncate(request.getErrorMessage(), 2048));

            auditLogRepository.save(builder.build());
        } catch (Exception ex) {
            // Audit logging must never break the main flow
            log.error("Failed to persist audit log entry", ex);
        }
    }

    /**
     * Synchronous record. Useful in test paths or when the caller wants to ensure
     * the log is written before the call returns.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSync(AuditLogRequest request) {
        record(request);
    }

    public CustomPairModel<Long, java.util.List<AuditLogResponseDto>> getRecentActivities(AuditLogFilterDto filterDto) {
        Page<AuditLogEntity> page = auditLogRepository.findAll(filterDto.createSpec(), filterDto.createPageable());
        return new CustomPairModel<>(page.getTotalElements(),
                auditLogMapper.entitiesToResponseDtos(page.getContent()));
    }

    public java.util.List<AuditLogResponseDto> getRecentActivities(int limit) {
        int size = limit <= 0 ? 20 : Math.min(limit, 200);
        return auditLogMapper.entitiesToResponseDtos(
                auditLogRepository.findAll(
                        org.springframework.data.domain.PageRequest.of(0, size,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                ).getContent());
    }

    public Map<UUID, String> resolveOrgNames() {
        return organizationRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(OrganizationEntity::getId, OrganizationEntity::getName, (a, b) -> a));
    }

    private String resolveOrgName(UUID orgId) {
        if (orgId == null) return null;
        return organizationRepository.findById(orgId).map(OrganizationEntity::getName).orElse(null);
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }

    public Page<AuditLogResponseDto> getAll(AuditLogFilterDto filterDto) {
        Page<AuditLogEntity> page = auditLogRepository.findAll(filterDto.createSpec(), filterDto.createPageable());
        return page.map(auditLogMapper::entityToResponseDto);
    }

    public AuditLogResponseDto getById(UUID id) {
        return auditLogRepository.findById(id)
                .map(auditLogMapper::entityToResponseDto)
                .orElse(null);
    }

   
}
