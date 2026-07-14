package ai.dto.own.request.report;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Filter for activity report")
public class ActivityReportFilterDto {

    @Schema(description = "Organization ID to filter by. If null, uses the current user's organization from JWT", example = "00000000-0000-0000-0000-000000000000")
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
}