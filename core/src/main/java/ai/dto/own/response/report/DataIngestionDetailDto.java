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
@Schema(description = "Detailed data ingestion statistics")
public class DataIngestionDetailDto {

    @Schema(description = "Breakdown by ingestion status (CREATED, EXTRACTING, COMPLETED, FAILED, etc.)")
    Map<String, Long> byStatus;

    @Schema(description = "Breakdown by data source (DOCUMENT, TOPIC, NOTEBOOK, SYSTEM)")
    Map<String, Long> bySource;

    @Schema(description = "Breakdown by access level (PERSONAL, LOCAL, GLOBAL)")
    Map<String, Long> byAccessLevel;

    @Schema(description = "Top N users with most ingestions")
    List<OwnerIngestionSummary> topOwners;

    @Schema(description = "Total file size in bytes", example = "104857600")
    long totalFileSize;

    @Schema(description = "Breakdown by content type (PDF, DOCX, PNG, etc.)")
    Map<String, Long> byContentType;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Owner ingestion summary")
    public static class OwnerIngestionSummary {

        @Schema(description = "User ID")
        UUID userId;

        @Schema(description = "User name", example = "nguyenvana")
        String userName;

        @Schema(description = "Number of ingestions", example = "25")
        long ingestionCount;

        @Schema(description = "Total file size in bytes", example = "52428800")
        long totalFileSize;
    }
}