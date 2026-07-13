package ai.dto.own.response.report;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Content statistics")
public class ContentStatsDto {

    @Schema(description = "Breakdown of drafts by type")
    Map<String, Long> draftsByType;

    @Schema(description = "Total number of topics", example = "30")
    long totalTopics;

    @Schema(description = "Total number of notebooks", example = "20")
    long totalNoteBooks;

    @Schema(description = "Total number of notes", example = "500")
    long totalNotes;

    @Schema(description = "Breakdown of notes by source type (MANUAL, AI, etc.)")
    Map<String, Long> notesBySourceType;
}