package ai.service.report;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import ai.dto.own.request.report.ComprehensiveReportFilterDto;
import ai.dto.own.response.report.ActivityReportResponseDto.DailyActivityDto;
import ai.dto.own.response.report.ActivityReportResponseDto.UserActivitySummary;
import ai.dto.own.response.report.ComprehensiveReportResponseDto;
import ai.entity.postgres.OrganizationEntity;
import ai.enums.ApiResponseStatus;
import ai.exception.AppException;
import ai.repository.AuditLogRepository;
import ai.repository.DataIngestionRepository;
import ai.repository.DraftRepository;
import ai.repository.NoteBookRepository;
import ai.repository.NoteRepository;
import ai.repository.OrganizationRepository;
import ai.repository.TopicRepository;
import ai.repository.UserRepository;
import ai.service.OrganizationService;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ComprehensiveReportService {

    UserRepository userRepository;
    OrganizationRepository orgRepository;
    OrganizationService organizationService;
    DraftRepository draftRepository;
    TopicRepository topicRepository;
    NoteBookRepository noteBookRepository;
    DataIngestionRepository dataIngestionRepository;
    NoteRepository noteRepository;
    AuditLogRepository auditLogRepository;

    /**
     * Lấy báo cáo tổng hợp tất cả chỉ số.
     */
    public ComprehensiveReportResponseDto getComprehensiveReport(ComprehensiveReportFilterDto filter) {
        UUID orgId = JwtUtil.getOrgId();
        if (orgId == null) {
            throw new AppException(ApiResponseStatus.ORG_ID_REQUIRED);
        }
        List<UUID> orgIds = resolveOrgIds(orgId, filter.isIncludeDescendants());
        Instant from = filter.getFrom();
        Instant to = filter.getTo();

        ComprehensiveReportResponseDto dto = new ComprehensiveReportResponseDto();

        // User metrics (global — toàn hệ thống)
        dto.setTotalUsers(userRepository.countAllUsers());
        dto.setTotalOrganizations(orgRepository.countAllOrganizations());

        // Content metrics (filtered by from-to)
        dto.setTotalDrafts(draftRepository.countByDateRange(orgIds, from, to));
        dto.setTotalTopics(topicRepository.countByDateRange(orgIds, from, to));
        dto.setTotalNoteBooks(noteBookRepository.countByDateRange(orgIds, from, to));
        dto.setTotalDataIngestions(dataIngestionRepository.countByDateRange(orgIds, from, to));
        dto.setTotalNotes(noteRepository.countByDateRange(orgIds, from, to));

        // Activity metrics (filtered by from-to)
        dto.setTotalActions(auditLogRepository.countActionsByOrgIdsAndDateRange(orgIds, from, to));
        dto.setTotalLogins(auditLogRepository.countLoginsByOrgIdsAndDateRange(orgIds, from, to));
        dto.setUniqueActiveUsers(auditLogRepository.countUniqueActiveUsersByOrgIdsAndDateRange(orgIds, from, to));

        // Top 10 active users (filtered by from-to)
        dto.setTopActiveUsers(buildTopActiveUsers(
                auditLogRepository.findTopActiveUsers(orgIds, from, to, PageRequest.of(0, 10))));

        // Daily trend (dùng from-to từ filter, fallback 7 ngày nếu null)
        dto.setRecentDailyTrend(buildDailyTrend(orgIds, from, to));

        return dto;
    }

    private List<UUID> resolveOrgIds(UUID orgId, boolean includeDescendants) {
        if (!includeDescendants) {
            return List.of(orgId);
        }
        OrganizationEntity org = organizationService.getEntityById(orgId);
        String pathPrefix = org.getPath() + "/%";
        return orgRepository.findDescendantOrgIds(orgId, pathPrefix);
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

    private List<DailyActivityDto> buildDailyTrend(List<UUID> orgIds, Instant from, Instant to) {
        // Nếu không có from-to, mặc định 7 ngày gần nhất
        if (from == null || to == null) {
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            LocalDate sevenDaysAgo = today.minusDays(6);
            from = sevenDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant();
            to = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        }

        List<Object[]> rows = auditLogRepository.findDailyActivityTrend(orgIds, from, to);
        List<DailyActivityDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            DailyActivityDto d = new DailyActivityDto();
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