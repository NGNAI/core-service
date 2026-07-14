package ai.dto.own.request.report;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Filter for comprehensive report")
public class ComprehensiveReportFilterDto {

    @NotNull
    @Schema(description = "Organization ID", example = "00000000-0000-0000-0000-000000000000")
    UUID orgId;

    @Schema(description = "Whether to include descendant organizations", example = "false", defaultValue = "false")
    boolean includeDescendants;

    @Schema(description = "Start date-time for activity data (ISO-8601)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant from;

    @Schema(description = "End date-time for activity data (ISO-8601)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant to;
}