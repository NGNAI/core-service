package ai.dto.own.request.report;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Filter for user report")
public class UserReportFilterDto {

    @Schema(description = "Organization ID to filter by. If null, uses the current user's organization from JWT", example = "00000000-0000-0000-0000-000000000000")
    UUID orgId;

    @Schema(description = "Whether to include descendant organizations in the report", example = "false", defaultValue = "false")
    boolean includeDescendants;

    @Schema(description = "Filter by active status. null = all, true = active only, false = inactive only", example = "true")
    Boolean active;
}