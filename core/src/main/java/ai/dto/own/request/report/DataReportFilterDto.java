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
@Schema(description = "Filter for data report")
public class DataReportFilterDto {

    @NotNull
    @Schema(description = "Organization ID", example = "00000000-0000-0000-0000-000000000000")
    UUID orgId;

    @Schema(description = "Whether to include descendant organizations", example = "false", defaultValue = "false")
    boolean includeDescendants;

    @Schema(description = "Start date-time (ISO-8601)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant from;

    @Schema(description = "End date-time (ISO-8601)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant to;

    @Schema(description = "Filter by ingestion status (CREATED, EXTRACTING, COMPLETED, FAILED, etc.)")
    String status;

    @Schema(description = "Filter by data source (SYSTEM, DOCUMENT, TOPIC, NOTEBOOK)")
    String source;

    @Schema(description = "Number of top owners to return", example = "10", defaultValue = "10")
    int topN = 10;
}