package ai.dto.outer.ingestion.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IngestionDeleteResponseDto {
    @JsonAlias({"file_id"})
    UUID fileId;

    @JsonAlias({"status"})
    String status;
}
