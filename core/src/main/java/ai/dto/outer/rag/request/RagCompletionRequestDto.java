package ai.dto.outer.rag.request;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * Base completion request dùng chung cho RAG API.
 * <p>
 * Chứa các field chung nhất: messages, stream, model, temperature, maxTokens.
 * <ul>
 *   <li>{@link TopicRagCompletionRequestDto} — chat Topic (metadata có topic_id)</li>
 *   <li>{@link NotebookRagCompletionRequestDto} — chat Notebook (metadata có notebook_id, user_instruction)</li>
 * </ul>
 * Class này cũng được dùng trực tiếp cho những completion thuần AI phụ trợ
 * (title, summary) không cần metadata đặc trưng.
 * <p>
 * Thiết kế kế thừa (super builder) giúp mở rộng future-proof: muốn thêm field mới
 * cho một loại (Topic/Notebook) chỉ cần thêm vào đúng subclass, không phải tạo
 * thêm class hay sửa payload gửi lên RAG (mọi field giữ nguyên {@code @JsonProperty}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagCompletionRequestDto {
    List<Message> messages;
    Metadata metadata;
    boolean stream;

    @JsonProperty("model")
    String model;

    @JsonProperty("temperature")
    Double temperature;

    @JsonProperty("max_tokens")
    Integer maxTokens;

    public RagCompletionRequestDto() {
        // no-arg cho Jackson
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Message {
        String role;
        String content;
    }

    /**
     * Metadata dùng chung. Các field đặc trưng theo loại được khai báo ở subclass
     * (topic_id ở Topic.Metadata, notebook_id + user_instruction ở Notebook.Metadata).
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @SuperBuilder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Metadata {
        @JsonProperty("user_id")
        UUID userId;

        @JsonProperty("organization_id")
        UUID organizationId;

        @JsonProperty("scopes")
        Set<String> scopes;

        @JsonProperty("file_ids")
        Set<String> fileIds = Set.of();

        @JsonProperty("summaries")
        String summaries;
    }
}
