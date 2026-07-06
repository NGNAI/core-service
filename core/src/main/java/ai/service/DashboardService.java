package ai.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ai.dto.own.request.filter.AuditLogFilterDto;
import ai.dto.own.request.filter.UserFilterDto;
import ai.dto.own.response.AuditLogResponseDto;
import ai.dto.own.response.dashboard.DashboardOverviewDto;
import ai.dto.own.response.dashboard.DataIngestionStatisticsDto;
import ai.dto.own.response.dashboard.DraftStatisticsDto;
import ai.dto.own.response.dashboard.RecentActivitiesDto;
import ai.dto.own.response.dashboard.TimelineStatisticsDto;
import ai.enums.DataScope;
import ai.enums.DataSource;
import ai.enums.IngestionStatus;
import ai.model.CustomPairModel;
import ai.repository.DataIngestionRepository;
import ai.repository.DraftRepository;
import ai.repository.NoteBookRepository;
import ai.repository.OrganizationRepository;
import ai.repository.TopicRepository;
import ai.repository.UserRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardService {

    DraftRepository draftRepository;
    DataIngestionRepository dataIngestionRepository;
    TopicRepository topicRepository;
    NoteBookRepository noteBookRepository;
    UserRepository userRepository;
    OrganizationRepository organizationRepository;
    AuditLogService auditLogService;
    OrganizationUserRoleService organizationUserRoleService;

    public DashboardOverviewDto getOverview() {
        return getOverview(null);
    }

    public DashboardOverviewDto getOverview(Boolean useGlobal) {
        DashboardOverviewDto dto = new DashboardOverviewDto();
        UUID orgId = JwtUtil.getOrgId();
        
        if (useGlobal == null || !useGlobal) {
            // Use org-specific data
            dto.setTotalDrafts(draftRepository.countAllDraftsByOrgId(orgId));
            dto.setTotalTopics(topicRepository.countAllTopicsByOrgId(orgId));
            dto.setTotalNoteBooks(noteBookRepository.countAllNoteBooksByOrgId(orgId));
            dto.setTotalDataIngestions(dataIngestionRepository.countAllDataIngestionsByOrgId(orgId));
            // For user count, we'll use the organization user role service to get count by org
            dto.setTotalUsers(organizationUserRoleService.getUsersByOrgId(orgId, new UserFilterDto()).getFirst());
            dto.setTotalOrganizations(organizationRepository.countAllOrganizations());
        } else {
            // Use global data
            dto.setTotalDrafts(draftRepository.countAllDrafts());
            dto.setTotalTopics(topicRepository.countAllTopics());
            dto.setTotalNoteBooks(noteBookRepository.countAllNoteBooks());
            dto.setTotalDataIngestions(dataIngestionRepository.countAllDataIngestions());
            dto.setTotalUsers(userRepository.countAllUsers());
            dto.setTotalOrganizations(organizationRepository.countAllOrganizations());
        }
        return dto;
    }

    public DraftStatisticsDto getDraftStatistics() {
        return getDraftStatistics(null);
    }

    public DraftStatisticsDto getDraftStatistics(Boolean useGlobal) {
        DraftStatisticsDto dto = new DraftStatisticsDto();
        UUID orgId = JwtUtil.getOrgId();
        
        if (useGlobal == null || !useGlobal) {
            // Use org-specific data
            dto.setTotalDrafts(draftRepository.countAllDraftsByOrgId(orgId));
            
            // Thống kê theo loại bản nháp
            Map<String, Long> draftsByType = new HashMap<>();
            draftRepository.countByTypeByOrgId(orgId).forEach(row -> {
                draftsByType.put((String) row[0], (Long) row[1]);
            });
            dto.setDraftsByType(draftsByType);
        } else {
            // Use global data
            dto.setTotalDrafts(draftRepository.countAllDrafts());
            
            // Thống kê theo loại bản nháp
            Map<String, Long> draftsByType = new HashMap<>();
            draftRepository.countByType().forEach(row -> {
                draftsByType.put((String) row[0], (Long) row[1]);
            });
            dto.setDraftsByType(draftsByType);
        }
        
        // // Thống kê theo phong cách trình bày
        // Map<String, Long> draftsByPresentationStyle = new HashMap<>();
        // draftRepository.countByPresentationStyle().forEach(row -> {
        //     draftsByPresentationStyle.put((String) row[0], (Long) row[1]);
        // });
        // dto.setDraftsByPresentationStyle(draftsByPresentationStyle);
        
        return dto;
    }

    public DataIngestionStatisticsDto getDataIngestionStatistics() {
        return getDataIngestionStatistics(null);
    }

    public DataIngestionStatisticsDto getDataIngestionStatistics(Boolean useGlobal) {
        DataIngestionStatisticsDto dto = new DataIngestionStatisticsDto();
        UUID orgId = JwtUtil.getOrgId();
        
        if (useGlobal == null || !useGlobal) {
            // Use org-specific data
            dto.setTotalDataIngestions(dataIngestionRepository.countAllDataIngestionsByOrgId(orgId));
            
            // Thống kê theo trạng thái
            Map<String, Long> ingestionsByStatus = new HashMap<>();
            dataIngestionRepository.countByStatusByOrgId(orgId).forEach(row -> {
                IngestionStatus status = (IngestionStatus) row[0];
                if (status != null) {
                    ingestionsByStatus.put(status.name(), (Long) row[1]);
                } else {
                    // Xử lý trường hợp status là null, có thể bỏ qua hoặc gán một giá trị mặc định
                    ingestionsByStatus.put("UNKNOWN", (Long) row[1]);
                }
            });
            dto.setIngestionsByStatus(ingestionsByStatus);
            
            // Thống kê theo nguồn dữ liệu
            Map<String, Long> ingestionsBySource = new HashMap<>();
            dataIngestionRepository.countBySourceByOrgId(orgId).forEach(row -> {
                ingestionsBySource.put(((DataSource) row[0]).name(), (Long) row[1]);
            });
            dto.setIngestionsBySource(ingestionsBySource);
            
            // Thống kê theo cấp độ truy cập
            Map<String, Long> ingestionsByAccessLevel = new HashMap<>();
            dataIngestionRepository.countByAccessLevelByOrgId(orgId).forEach(row -> {
                ingestionsByAccessLevel.put(((DataScope) row[0]).name(), (Long) row[1]);
            });
            dto.setIngestionsByAccessLevel(ingestionsByAccessLevel);
        } else {
            // Use global data
            dto.setTotalDataIngestions(dataIngestionRepository.countAllDataIngestions());
            
            // Thống kê theo trạng thái
            Map<String, Long> ingestionsByStatus = new HashMap<>();
            dataIngestionRepository.countByStatus().forEach(row -> {
                IngestionStatus status = (IngestionStatus) row[0];
                if (status != null) {
                    ingestionsByStatus.put(status.name(), (Long) row[1]);
                } else {
                    // Xử lý trường hợp status là null, có thể bỏ qua hoặc gán một giá trị mặc định
                    ingestionsByStatus.put("UNKNOWN", (Long) row[1]);
                }
            });
            dto.setIngestionsByStatus(ingestionsByStatus);
            
            // Thống kê theo nguồn dữ liệu
            Map<String, Long> ingestionsBySource = new HashMap<>();
            dataIngestionRepository.countBySource().forEach(row -> {
                ingestionsBySource.put(((DataSource) row[0]).name(), (Long) row[1]);
            });
            dto.setIngestionsBySource(ingestionsBySource);
            
            // Thống kê theo cấp độ truy cập
            Map<String, Long> ingestionsByAccessLevel = new HashMap<>();
            dataIngestionRepository.countByAccessLevel().forEach(row -> {
                ingestionsByAccessLevel.put(((DataScope) row[0]).name(), (Long) row[1]);
            });
            dto.setIngestionsByAccessLevel(ingestionsByAccessLevel);
        }
        
        return dto;
    }

    public TimelineStatisticsDto getTimelineStatistics(LocalDate from, LocalDate to) {
        return getTimelineStatistics(from, to, null);
    }

    public TimelineStatisticsDto getTimelineStatistics(LocalDate from, LocalDate to, Boolean useGlobal) {
        TimelineStatisticsDto dto = new TimelineStatisticsDto();
        UUID orgId = JwtUtil.getOrgId();
        
        // Tính số ngày giữa hai ngày
        long daysBetween = ChronoUnit.DAYS.between(from, to);
        List<TimelineStatisticsDto.DailyStatisticsDto> dailyStats = new ArrayList<>();
        
        // Tạo dữ liệu thống kê cho từng ngày
        for (int i = 0; i <= daysBetween; i++) {
            LocalDate date = from.plusDays(i);
            TimelineStatisticsDto.DailyStatisticsDto dailyStat = new TimelineStatisticsDto.DailyStatisticsDto();
            dailyStat.setDate(date.toString());
            
            // Nếu useGlobal là true hoặc null, lấy dữ liệu toàn hệ thống
            if (useGlobal == null || useGlobal) {
                // Lấy số lượng bản nháp theo ngày
                dailyStat.setDraftCount(draftRepository.countDraftsByDate(date));
                
                // Lấy số lượng chủ đề theo ngày
                dailyStat.setTopicCount(topicRepository.countTopicsByDate(date));
                
                // Lấy số lượng notebook theo ngày
                dailyStat.setNoteBookCount(noteBookRepository.countNoteBooksByDate(date));
                
                // Lấy số lượng dữ liệu nhập theo ngày
                dailyStat.setDataIngestionCount(dataIngestionRepository.countDataIngestionsByDate(date));
            } else {
                // Lấy dữ liệu theo tổ chức
                dailyStat.setDraftCount(draftRepository.countDraftsByDateAndOrgId(date, orgId));
                
                // Lấy số lượng chủ đề theo ngày
                dailyStat.setTopicCount(topicRepository.countTopicsByDateAndOrgId(date, orgId));
                
                // Lấy số lượng notebook theo ngày
                dailyStat.setNoteBookCount(noteBookRepository.countNoteBooksByDateAndOrgId(date, orgId));
                
                // Lấy số lượng dữ liệu nhập theo ngày
                dailyStat.setDataIngestionCount(dataIngestionRepository.countDataIngestionsByDateAndOrgId(date, orgId));
            }
            
            dailyStats.add(dailyStat);
        }
        
        dto.setDailyStatistics(dailyStats);
        return dto;
    }

    /**
     * Lấy các hoạt động gần đây nhất trong hệ thống dựa trên audit log.
     * Hỗ trợ filter theo userId, orgId, action, resource, status, khoảng thời gian và keyword.
     */
    public RecentActivitiesDto getRecentActivities(AuditLogFilterDto filterDto) {
        return getRecentActivities(filterDto, null);
    }

    public RecentActivitiesDto getRecentActivities(AuditLogFilterDto filterDto, Boolean useGlobal) {
        UUID orgId = JwtUtil.getOrgId();
        
        // If not using global access, set orgId in filterDto
        if (useGlobal == null || !useGlobal) {
            if (filterDto.getOrgId() == null) {
                filterDto.setOrgId(orgId);
            }
        }
        
        CustomPairModel<Long, List<AuditLogResponseDto>> result = auditLogService.getRecentActivities(filterDto);
        RecentActivitiesDto dto = new RecentActivitiesDto();
        dto.setTotal(result.getFirst());
        dto.setItems(result.getSecond());
        return dto;
    }
}