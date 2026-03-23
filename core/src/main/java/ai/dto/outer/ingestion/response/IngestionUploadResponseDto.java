package ai.dto.outer.ingestion.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IngestionUploadResponseDto {
    @JsonProperty("job_id")
    UUID jobId;
}
