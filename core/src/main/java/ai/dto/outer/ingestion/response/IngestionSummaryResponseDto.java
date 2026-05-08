package ai.dto.outer.ingestion.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IngestionSummaryResponseDto {

    @JsonProperty("file_id")
    String fileId;

    @JsonProperty("summary")
    String summary;

    @JsonProperty("chunk_count")
    long chunkCount;

    @JsonProperty("method")
    String method;

    @JsonProperty("created_at")
    String createdAt;
}
