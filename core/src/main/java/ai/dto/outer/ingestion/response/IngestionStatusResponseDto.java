package ai.dto.outer.ingestion.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IngestionStatusResponseDto {
    @JsonAlias({"id", "job_id"})
    UUID jobId;

    @JsonAlias({"file_id", "data_ingestion_id"})
    UUID dataIngestionId;

    String filename;

    Meta meta;

    @JsonAlias({"status", "rag_status", "ingestion_status"})
    String status;

    @JsonProperty("created_at")
    String createdAt;

    @JsonProperty("updated_at")
    String updatedAt;

    @JsonAlias({"message", "error"})
    String message;

    @JsonProperty("total_chunks")
    Integer totalChunks;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Meta {
        String username;
        String unit;
        String visibility;
    }
}
