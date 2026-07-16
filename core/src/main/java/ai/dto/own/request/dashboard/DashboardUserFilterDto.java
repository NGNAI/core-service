package ai.dto.own.request.dashboard;

import java.time.Duration;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Filter for user dashboard")
public class DashboardUserFilterDto {

    @Schema(description = "Start date-time for activity data (ISO-8601, e.g. 2026-01-01T00:00:00Z)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant from;

    @Schema(description = "End date-time for activity data (ISO-8601, e.g. 2026-12-31T23:59:59Z)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant to;

    @AssertTrue(message = "Khoảng thời gian from-to không được vượt quá 90 ngày")
    @Schema(hidden = true)
    boolean isDateRangeValid() {
        if (from == null || to == null) return true;
        return Duration.between(from, to).toDays() <= 90;
    }
}