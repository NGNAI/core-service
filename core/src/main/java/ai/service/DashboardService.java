package ai.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ai.dto.own.response.dashboard.DashboardOverviewDto;
import ai.dto.own.response.dashboard.DataIngestionStatisticsDto;
import ai.dto.own.response.dashboard.DraftStatisticsDto;
import ai.dto.own.response.dashboard.TimelineStatisticsDto;
import ai.enums.DataScope;
import ai.enums.DataSource;
import ai.enums.IngestionStatus;
import ai.repository.DataIngestionRepository;
import ai.repository.DraftRepository;
import ai.repository.NoteBookRepository;
import ai.repository.OrganizationRepository;
import ai.repository.TopicRepository;
import ai.repository.UserRepository;
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

    public DashboardOverviewDto getOverview() {
        DashboardOverviewDto dto = new DashboardOverviewDto();
        dto.setTotalDrafts(draftRepository.countAllDrafts());
        dto.setTotalTopics(topicRepository.countAllTopics());
        dto.setTotalNoteBooks(noteBookRepository.countAllNoteBooks());
        dto.setTotalDataIngestions(dataIngestionRepository.countAllDataIngestions());
        dto.setTotalUsers(userRepository.countAllUsers());
        dto.setTotalOrganizations(organizationRepository.countAllOrganizations());
        return dto;
    }

    public DraftStatisticsDto getDraftStatistics() {
        DraftStatisticsDto dto = new DraftStatisticsDto();
        dto.setTotalDrafts(draftRepository.countAllDrafts());
        
        // Thống kê theo loại bản nháp
        Map<String, Long> draftsByType = new HashMap<>();
        draftRepository.countByType().forEach(row -> {
            draftsByType.put((String) row[0], (Long) row[1]);
        });
        dto.setDraftsByType(draftsByType);
        
        // Thống kê theo phong cách trình bày
        Map<String, Long> draftsByPresentationStyle = new HashMap<>();
        draftRepository.countByPresentationStyle().forEach(row -> {
            draftsByPresentationStyle.put((String) row[0], (Long) row[1]);
        });
        dto.setDraftsByPresentationStyle(draftsByPresentationStyle);
        
        return dto;
    }

    public DataIngestionStatisticsDto getDataIngestionStatistics() {
        DataIngestionStatisticsDto dto = new DataIngestionStatisticsDto();
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
        
        return dto;
    }

    public TimelineStatisticsDto getTimelineStatistics(LocalDate from, LocalDate to) {
        TimelineStatisticsDto dto = new TimelineStatisticsDto();
        
        // Tính số ngày giữa hai ngày
        long daysBetween = ChronoUnit.DAYS.between(from, to);
        List<TimelineStatisticsDto.DailyStatisticsDto> dailyStats = new ArrayList<>();
        
        // Tạo dữ liệu thống kê cho từng ngày
        for (int i = 0; i <= daysBetween; i++) {
            LocalDate date = from.plusDays(i);
            TimelineStatisticsDto.DailyStatisticsDto dailyStat = new TimelineStatisticsDto.DailyStatisticsDto();
            dailyStat.setDate(date.toString());
            // Các giá trị thống kê cho ngày này sẽ được tính sau
            dailyStats.add(dailyStat);
        }
        
        dto.setDailyStatistics(dailyStats);
        return dto;
    }
}