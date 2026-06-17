package ai.dto.outer.rag.request;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagCompletionRequestDto {
    List<Message> messages;
    Metadata metadata;
    boolean stream;

    @Data
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Message {
        String role;
        String content;
    }

    @Data
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Metadata {
        @JsonProperty("user_id")
        UUID userId;
        
        @JsonProperty("organization_id")
        UUID organizationId;

        @JsonProperty("topic_id")
        UUID topic_id;

        @JsonProperty("notebook_id")
        UUID notebook_id;

        @JsonProperty("draft_id")
        UUID draft_id;

        @JsonProperty("scopes")
        Set<String> scopes;
        
        @JsonProperty("file_ids")
        Set<String> fileIds;
        
        @JsonProperty("summaries")
        String summaries;
        
        @JsonProperty("user_instruction")
        String userInstruction;

        @JsonProperty("draft_settings")
        DraftSettings draftSettings;
        @Data
        @NoArgsConstructor
        @FieldDefaults(level = AccessLevel.PRIVATE)
        public static class DraftSettings {
            @JsonProperty("draft_id")
            UUID draftId;

            @JsonProperty("type")
            String type;

            @JsonProperty("presentation_style")
            String presentationStyle;

            @JsonProperty("language")
            String language;

            @JsonProperty("title")
            String title;

            @JsonProperty("detailed_description")
            String detailedDescription;

            @JsonProperty("generated_content")
            String generatedContent;
        }
    }
}
