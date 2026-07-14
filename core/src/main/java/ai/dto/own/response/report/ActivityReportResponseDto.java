package ai.dto.own.response.report;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Activity report response")
public class ActivityReportResponseDto {

    @Schema(description = "Total number of actions in the period", example = "1500")
    long totalActions;

    @Schema(description = "Total number of logins in the period", example = "120")
    long totalLogins;

    @Schema(description = "Number of unique active users", example = "45")
    long uniqueActiveUsers;

    @Schema(description = "Breakdown of actions by resource type (e.g., DRAFT, USER, ORG)")
    Map<String, Long> actionsByResource;

    @Schema(description = "Breakdown of actions by action type (e.g., CREATE, UPDATE, DELETE)")
    Map<String, Long> actionsByAction;

    @Schema(description = "Top N most active users")
    List<UserActivitySummary> topActiveUsers;

    @Schema(description = "Login frequency by user")
    List<LoginFrequencySummary> loginFrequency;

    @Schema(description = "Daily activity trend")
    List<DailyActivityDto> dailyTrend;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "User activity summary")
    public static class UserActivitySummary {

        @Schema(description = "User ID")
        UUID userId;

        @Schema(description = "User name", example = "nguyenvana")
        String userName;

        @Schema(description = "Total action count", example = "120")
        long actionCount;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Login frequency summary")
    public static class LoginFrequencySummary {

        @Schema(description = "User ID")
        UUID userId;

        @Schema(description = "User name", example = "nguyenvana")
        String userName;

        @Schema(description = "Login count", example = "15")
        long loginCount;

        @Schema(description = "Last login timestamp", example = "2026-07-13T10:30:00Z")
        String lastLogin;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Daily activity statistics")
    public static class DailyActivityDto {

        @Schema(description = "Date in format yyyy-MM-dd", example = "2026-07-01")
        String date;

        @Schema(description = "Total action count on this date", example = "50")
        long actionCount;

        @Schema(description = "Login count on this date", example = "10")
        long loginCount;
    }
}