package ai.dto.own.response.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Data report response - overview of all content types")
public class DataReportResponseDto {

    @Schema(description = "Total number of data ingestions", example = "150")
    long totalDataIngestions;

    @Schema(description = "Total number of drafts", example = "80")
    long totalDrafts;

    @Schema(description = "Total number of topics", example = "30")
    long totalTopics;

    @Schema(description = "Total number of notebooks", example = "20")
    long totalNoteBooks;

    @Schema(description = "Total number of notes", example = "500")
    long totalNotes;

    @Schema(description = "Detailed ingestion statistics")
    DataIngestionDetailDto ingestionDetail;

    @Schema(description = "Content statistics (drafts, topics, notebooks, notes)")
    ContentStatsDto contentStats;
}