package ai.service.report;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import ai.dto.own.request.report.ActivityReportFilterDto;
import ai.dto.own.response.report.ActivityReportResponseDto;
import ai.dto.own.response.report.ActivityReportResponseDto.DailyActivityDto;
import ai.dto.own.response.report.ActivityReportResponseDto.LoginFrequencySummary;
import ai.dto.own.response.report.ActivityReportResponseDto.UserActivitySummary;
import ai.entity.postgres.OrganizationEntity;
import ai.enums.ApiResponseStatus;
import ai.exception.AppException;
import ai.repository.AuditLogRepository;
import ai.repository.OrganizationRepository;
import ai.service.OrganizationService;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ActivityReportService {

    AuditLogRepository auditLogRepository;
    OrganizationRepository orgRepository;
    OrganizationService organizationService;

    /**
     * Lấy báo cáo tổng quan hoạt động trong kỳ.
     */
    public ActivityReportResponseDto getActivityReport(ActivityReportFilterDto filter) {
        UUID orgId = resolveOrgId(filter.getOrgId());
        List<UUID> orgIds = resolveOrgIds(orgId, filter.isIncludeDescendants());
        Instant from = filter.getFrom();
        Instant to = filter.getTo();

        ActivityReportResponseDto dto = new ActivityReportResponseDto();

        // 1. Tổng số actions
        dto.setTotalActions(auditLogRepository.countActionsByOrgIdsAndDateRange(orgIds, from, to));

        // 2. Tổng số logins
        dto.setTotalLogins(auditLogRepository.countLoginsByOrgIdsAndDateRange(orgIds, from, to));

        // 3. Số user hoạt động duy nhất
        dto.setUniqueActiveUsers(auditLogRepository.countUniqueActiveUsersByOrgIdsAndDateRange(orgIds, from, to));

        // 4. Actions theo resource
        dto.setActionsByResource(buildResourceMap(auditLogRepository.countActionsGroupByResource(orgIds, from, to)));

        // 5. Actions theo action type
        dto.setActionsByAction(buildResourceMap(auditLogRepository.countActionsGroupByAction(orgIds, from, to)));

        // 6. Top N users tích cực
        dto.setTopActiveUsers(buildTopActiveUsers(
                auditLogRepository.findTopActiveUsers(orgIds, from, to, PageRequest.of(0, filter.getTopN()))));

        // 7. Tần suất đăng nhập
        dto.setLoginFrequency(buildLoginFrequency(
                auditLogRepository.findLoginFrequency(orgIds, from, to)));

        // 8. Xu hướng theo ngày
        dto.setDailyTrend(buildDailyTrend(
                auditLogRepository.findDailyActivityTrend(orgIds, from, to)));

        return dto;
    }

    /**
     * Lấy danh sách top N người dùng tích cực nhất.
     */
    public List<UserActivitySummary> getTopActiveUsers(ActivityReportFilterDto filter) {
        UUID orgId = resolveOrgId(filter.getOrgId());
        List<UUID> orgIds = resolveOrgIds(orgId, filter.isIncludeDescendants());

        return buildTopActiveUsers(
                auditLogRepository.findTopActiveUsers(orgIds, filter.getFrom(), filter.getTo(), PageRequest.of(0, filter.getTopN())));
    }

    /**
     * Lấy tần suất đăng nhập của người dùng.
     */
    public List<LoginFrequencySummary> getLoginFrequency(ActivityReportFilterDto filter) {
        UUID orgId = resolveOrgId(filter.getOrgId());
        List<UUID> orgIds = resolveOrgIds(orgId, filter.isIncludeDescendants());

        return buildLoginFrequency(
                auditLogRepository.findLoginFrequency(orgIds, filter.getFrom(), filter.getTo()));
    }

    private UUID resolveOrgId(UUID orgId) {
        UUID result = orgId != null ? orgId : JwtUtil.getOrgId();
        if (result == null) {
            throw new AppException(ApiResponseStatus.ORG_ID_REQUIRED);
        }
        return result;
    }

    private List<UUID> resolveOrgIds(UUID orgId, boolean includeDescendants) {
        if (!includeDescendants) {
            return List.of(orgId);
        }
        OrganizationEntity org = organizationService.getEntityById(orgId);
        String pathPrefix = org.getPath() + "/%";
        return orgRepository.findDescendantOrgIds(orgId, pathPrefix);
    }

    private Map<String, Long> buildResourceMap(List<Object[]> rows) {
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put(row[0] != null ? row[0].toString() : "UNKNOWN", ((Number) row[1]).longValue());
        }
        return result;
    }

    private List<UserActivitySummary> buildTopActiveUsers(List<Object[]> rows) {
        List<UserActivitySummary> result = new ArrayList<>();
        for (Object[] row : rows) {
            UserActivitySummary s = new UserActivitySummary();
            s.setUserId((UUID) row[0]);
            s.setUserName((String) row[1]);
            s.setActionCount(((Number) row[2]).longValue());
            result.add(s);
        }
        return result;
    }

    private List<LoginFrequencySummary> buildLoginFrequency(List<Object[]> rows) {
        List<LoginFrequencySummary> result = new ArrayList<>();
        for (Object[] row : rows) {
            LoginFrequencySummary s = new LoginFrequencySummary();
            s.setUserId((UUID) row[0]);
            s.setUserName((String) row[1]);
            s.setLoginCount(((Number) row[2]).longValue());
            if (row[3] != null) {
                s.setLastLogin(((Instant) row[3]).toString());
            }
            result.add(s);
        }
        return result;
    }

    private List<DailyActivityDto> buildDailyTrend(List<Object[]> rows) {
        List<DailyActivityDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            DailyActivityDto d = new DailyActivityDto();
            // Row[0] is java.sql.Date from FUNCTION('DATE', ...)
            if (row[0] instanceof java.sql.Date) {
                d.setDate(((java.sql.Date) row[0]).toLocalDate().toString());
            } else {
                d.setDate(row[0] != null ? row[0].toString() : "UNKNOWN");
            }
            d.setActionCount(((Number) row[1]).longValue());
            d.setLoginCount(((Number) row[2]).longValue());
            result.add(d);
        }
        return result;
    }
}