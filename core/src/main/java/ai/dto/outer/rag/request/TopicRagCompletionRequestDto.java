package ai.dto.outer.rag.request;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * Completion request riêng cho Topic chat.
 * <p>
 * Kế thừa toàn bộ field dùng chung từ {@link RagCompletionRequestDto} (base).
 * Chỉ khai báo ở đây các field đặc trưng của Topic — mở rộng future-proof:
 * muốn thêm field Topic mới chỉ cần thêm vào class này hoặc vào Metadata bên dưới,
 * không phải tách thêm class nữa.
 */
@Getter
@Setter
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicRagCompletionRequestDto extends RagCompletionRequestDto {

    public TopicRagCompletionRequestDto() {
        // no-arg cho Jackson
    }

    /**
     * Metadata riêng của Topic. Kế thừa các field metadata dùng chung
     * (user_id, organization_id, scopes, file_ids, summaries) từ base.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @SuperBuilder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Metadata extends RagCompletionRequestDto.Metadata {
        @JsonProperty("topic_id")
        UUID topicId;
    }
}
