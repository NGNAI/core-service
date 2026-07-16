package ai.service.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ai.dto.own.request.dashboard.DashboardUserFilterDto;
import ai.dto.own.response.dashboard.DashboardUserResponseDto;
import ai.dto.own.response.dashboard.DashboardUserResponseDto.DailyActivityDto;
import ai.enums.ApiResponseStatus;
import ai.exception.AppException;
import ai.repository.AuditLogRepository;
import ai.repository.DataIngestionRepository;
import ai.repository.NoteBookRepository;
import ai.repository.NoteRepository;
import ai.repository.TopicRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardUserService {

    TopicRepository topicRepository;
    NoteBookRepository noteBookRepository;
    NoteRepository noteRepository;
    DataIngestionRepository dataIngestionRepository;
    AuditLogRepository auditLogRepository;

    /**
     * Lấy dashboard tổng quan cho người dùng hiện tại.
     * - orgId, userId lấy từ JWT token.
     * - Activity trend dùng khoảng from-to (mặc định 7 ngày gần nhất nếu không truyền).
     */
    public DashboardUserResponseDto getUserDashboard(DashboardUserFilterDto filter) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();
        if (userId == null || orgId == null) {
            throw new AppException(ApiResponseStatus.ORG_ID_REQUIRED);
        }

        Instant from = filter.getFrom();
        Instant to = filter.getTo();

        // Nếu không có from-to, mặc định 7 ngày gần nhất
        if (from == null || to == null) {
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            LocalDate sevenDaysAgo = today.minusDays(6);
            from = sevenDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant();
            to = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        }

        DashboardUserResponseDto dto = new DashboardUserResponseDto();

        // 1. Topic: tổng số
        dto.setTotalTopics(topicRepository.countByOwnerId(userId, orgId));

        // 2. Notebook: tổng số
        dto.setTotalNoteBooks(noteBookRepository.countByOwnerId(userId, orgId));

        // 3. Note: tổng số và nhóm theo loại (sourceType)
        dto.setTotalNotes(noteRepository.countByOwnerId(userId, orgId));
        dto.setNotesBySourceType(buildNotesBySourceType(userId, orgId));

        // 4. DataIngestion: tổng số và theo trạng thái
        dto.setTotalDataIngestions(dataIngestionRepository.countByOwnerId(userId, orgId));
        dto.setDataIngestionsByStatus(buildDataIngestionsByStatus(userId, orgId));

        // 5. Activity: 7 ngày gần nhất (hoặc theo param from-to)
        dto.setTotalActions(auditLogRepository.countActionsByUserIdAndDateRange(userId, orgId, from, to));
        dto.setTotalLogins(auditLogRepository.countLoginsByUserIdAndDateRange(userId, orgId, from, to));
        dto.setDailyActivityTrend(buildDailyActivityTrend(userId, orgId, from, to));

        return dto;
    }

    private Map<String, Long> buildNotesBySourceType(UUID userId, UUID orgId) {
        Map<String, Long> result = new HashMap<>();
        noteRepository.countBySourceTypeByOwnerId(userId, orgId).forEach(row -> {
            Object sourceType = row[0];
            result.put(sourceType != null ? sourceType.toString() : "UNKNOWN", (Long) row[1]);
        });
        return result;
    }

    private Map<String, Long> buildDataIngestionsByStatus(UUID userId, UUID orgId) {
        Map<String, Long> result = new HashMap<>();
        dataIngestionRepository.countByStatusByOwnerId(userId, orgId).forEach(row -> {
            Object status = row[0];
            result.put(status != null ? status.toString() : "UNKNOWN", ((Number) row[1]).longValue());
        });
        return result;
    }

    private List<DailyActivityDto> buildDailyActivityTrend(UUID userId, UUID orgId, Instant from, Instant to) {
        List<Object[]> rows = auditLogRepository.findDailyActivityTrendByUserId(userId, orgId, from, to);

        // Build map date -> [actionCount, loginCount] từ kết quả query
        Map<String, long[]> dataByDate = new HashMap<>();
        for (Object[] row : rows) {
            String dateKey;
            if (row[0] instanceof java.sql.Date) {
                dateKey = ((java.sql.Date) row[0]).toLocalDate().toString();
            } else {
                dateKey = row[0] != null ? row[0].toString() : "UNKNOWN";
            }
            long actionCount = ((Number) row[1]).longValue();
            long loginCount = ((Number) row[2]).longValue();
            dataByDate.put(dateKey, new long[]{actionCount, loginCount});
        }

        // Fill đầy tất cả ngày trong range (ngày không có data thì = 0)
        List<DailyActivityDto> result = new ArrayList<>();
        LocalDate startDate = from.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = to.atZone(ZoneId.systemDefault()).toLocalDate().minusDays(1); // trừ 1 vì `to` là startOfDay của ngày kế tiếp
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dateKey = date.toString();
            DailyActivityDto d = new DailyActivityDto();
            d.setDate(dateKey);
            long[] counts = dataByDate.get(dateKey);
            d.setActionCount(counts != null ? counts[0] : 0);
            d.setLoginCount(counts != null ? counts[1] : 0);
            result.add(d);
        }
        return result;
    }
}