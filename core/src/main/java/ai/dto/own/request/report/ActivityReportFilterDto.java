package ai.dto.own.request.report;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Filter for activity report")
public class ActivityReportFilterDto {

    @NotNull
    @Schema(description = "Organization ID", example = "00000000-0000-0000-0000-000000000000")
    UUID orgId;

    @Schema(description = "Whether to include descendant organizations", example = "false", defaultValue = "false")
    boolean includeDescendants;

    @Schema(description = "Start date-time (ISO-8601, e.g. 2026-01-01T00:00:00Z)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant from;

    @Schema(description = "End date-time (ISO-8601, e.g. 2026-12-31T23:59:59Z)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant to;

    @Schema(description = "Number of top active users to return", example = "10", defaultValue = "10")
    int topN = 10;

    @AssertTrue(message = "Khoảng thời gian from-to không được vượt quá 90 ngày")
    @Schema(hidden = true)
    boolean isDateRangeValid() {
        if (from == null || to == null) return true;
        return Duration.between(from, to).toDays() <= 90;
    }
}