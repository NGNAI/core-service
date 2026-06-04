package ai.dto.own.response.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Dashboard overview statistics")
public class DashboardOverviewDto {
    
    @Schema(description = "Total number of drafts", example = "42")
    long totalDrafts;
    
    @Schema(description = "Total number of topics", example = "15")
    long totalTopics;
    
    @Schema(description = "Total number of notebooks", example = "8")
    long totalNoteBooks;
    
    @Schema(description = "Total number of data ingestions", example = "27")
    long totalDataIngestions;
    
    @Schema(description = "Total number of users", example = "5")
    long totalUsers;
    
    @Schema(description = "Total number of organizations", example = "2")
    long totalOrganizations;
}