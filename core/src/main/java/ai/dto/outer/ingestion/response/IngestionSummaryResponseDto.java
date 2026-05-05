package ai.dto.outer.ingestion.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IngestionSummaryResponseDto {
    @JsonProperty("summary")
    String summary;
}
