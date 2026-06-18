package ai.dto.own.response.dashboard;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Draft statistics")
public class DraftStatisticsDto {
    
    @Schema(description = "Total number of drafts", example = "42")
    long totalDrafts;
    
    @Schema(description = "Breakdown of drafts by type")
    Map<String, Long> draftsByType;
}