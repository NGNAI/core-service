package ai.dto.outer.rag.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagCompletionRequestDto {
    String model;
    List<Message> messages;

    @JsonProperty("top_k")
    int topK;
    boolean stream;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Message {
        String role;
        String content;
    }
}
