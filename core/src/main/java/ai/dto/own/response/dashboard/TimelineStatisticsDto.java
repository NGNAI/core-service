package ai.dto.own.response.dashboard;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Timeline statistics")
public class TimelineStatisticsDto {
    
    @Schema(description = "List of daily statistics")
    List<DailyStatisticsDto> dailyStatistics;
    
    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Daily statistics for a specific date")
    public static class DailyStatisticsDto {
        
        @Schema(description = "Date in format yyyy-MM-dd", example = "2026-06-01")
        String date;
        
        @Schema(description = "Number of drafts created on this date", example = "5")
        long draftCount;
        
        @Schema(description = "Number of topics created on this date", example = "2")
        long topicCount;
        
        @Schema(description = "Number of notebooks created on this date", example = "1")
        long noteBookCount;
        
        @Schema(description = "Number of data ingestions created on this date", example = "3")
        long dataIngestionCount;
    }
}