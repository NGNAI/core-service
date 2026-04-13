package ai.dto.outer.rag.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagCompletionRequestDto {
    String model;
    List<Message> messages;
    List<Message> history;
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
        UUID userId;
        UUID organizationId;
        Set<String> scopes;
        Set<UUID> fileIds;
    }
}
