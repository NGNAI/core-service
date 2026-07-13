package ai.dto.own.response.report;

import java.util.List;

import ai.dto.own.response.report.ActivityReportResponseDto.DailyActivityDto;
import ai.dto.own.response.report.ActivityReportResponseDto.UserActivitySummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Comprehensive report response - all metrics in one response")
public class ComprehensiveReportResponseDto {

    // User metrics
    @Schema(description = "Total number of users", example = "100")
    long totalUsers;

    @Schema(description = "Number of active users", example = "80")
    long activeUsers;

    @Schema(description = "Number of inactive users", example = "20")
    long inactiveUsers;

    @Schema(description = "Total number of organizations", example = "5")
    long totalOrganizations;

    // Content metrics
    @Schema(description = "Total number of drafts", example = "80")
    long totalDrafts;

    @Schema(description = "Total number of topics", example = "30")
    long totalTopics;

    @Schema(description = "Total number of notebooks", example = "20")
    long totalNoteBooks;

    @Schema(description = "Total number of data ingestions", example = "150")
    long totalDataIngestions;

    @Schema(description = "Total number of notes", example = "500")
    long totalNotes;

    // Activity metrics
    @Schema(description = "Total number of actions in the period", example = "1500")
    long totalActions;

    @Schema(description = "Total number of logins in the period", example = "120")
    long totalLogins;

    @Schema(description = "Number of unique active users in the period", example = "45")
    long uniqueActiveUsers;

    @Schema(description = "Top 10 most active users")
    List<UserActivitySummary> topActiveUsers;

    @Schema(description = "Daily activity trend for the last 7 days")
    List<DailyActivityDto> recentDailyTrend;
}