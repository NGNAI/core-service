package ai.dto.outer.rag.request;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagCompletionRequestDto {
    List<Message> messages;
    Metadata metadata;
    boolean stream;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Message {
        String role;
        String content;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Metadata {
        @JsonProperty("user_id")
        UUID userId;
        @JsonProperty("organization_id")
        UUID organizationId;
        Set<String> scopes;
        @JsonProperty("file_ids")
        Set<String> fileIds;
    }
}
