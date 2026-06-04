package ai.dto.own.response.dashboard;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Data ingestion statistics")
public class DataIngestionStatisticsDto {
    
    @Schema(description = "Total number of data ingestions", example = "27")
    long totalDataIngestions;
    
    @Schema(description = "Breakdown of data ingestions by status (e.g., CREATED, EXTRACTING, COMPLETED, etc.)")
    Map<String, Long> ingestionsByStatus;
    
    @Schema(description = "Breakdown of data ingestions by source")
    Map<String, Long> ingestionsBySource;
    
    @Schema(description = "Breakdown of data ingestions by access level")
    Map<String, Long> ingestionsByAccessLevel;
}