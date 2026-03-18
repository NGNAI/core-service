package ai.dto.outer.ingestion.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IngestionUploadResponseDto {
    @JsonProperty("job_id")
    UUID jobId;
}
