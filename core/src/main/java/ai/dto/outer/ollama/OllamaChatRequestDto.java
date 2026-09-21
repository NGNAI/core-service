package ai.dto.outer.ollama;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
/**
 * Request body cho native Ollama API {@code POST /api/chat}.
 *
 * <p>Khác với endpoint OpenAI-compatible, Ollama dùng {@code options} để chứa
 * tham số sinh văn bản và {@code format} để buộc định dạng output.
 *
 * @see <a href="https://github.com/ollama/ollama/blob/main/docs/api.md">Ollama API</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OllamaChatRequestDto {

    /** Tên model trên Ollama, ví dụ {@code deepseek-v4-flash:cloud}. */
    String model;

    /** Danh sách message theo format chat. */
    List<OllamaMessage> messages;

    /**
     * {@code false} = trả về một response JSON duy nhất thay vì stream NDJSON.
     * Autocomplete luôn dùng {@code false}.
     */
    Boolean stream;

    /**
     * Định dạng output mong muốn. {@code "json"} buộc Ollama đảm bảo output là
     * JSON hợp lệ (structured output) — dùng cho tác vụ trả về mảng gợi ý.
     */
    String format;

    /**
     * Bật/tắt chế độ suy luận với các model thinking. Tắt để giảm latency.
     * Model không hỗ trợ sẽ bỏ qua field này.
     */
    Boolean think;

    /** Thời gian giữ model trong RAM sau khi sinh xong (vd {@code "10m"}). */
    @JsonProperty("keep_alive")
    String keepAlive;

    /** Tham số sinh văn bản của Ollama. */
    Options options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OllamaMessage {
        /** {@code system} | {@code user} | {@code assistant}. */
        String role;
        String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Options {
        Double temperature;

        /** Số token tối đa được sinh ra. */
        @JsonProperty("num_predict")
        Integer numPredict;

        /** Kích thước context window. */
        @JsonProperty("num_ctx")
        Integer numCtx;
    }
}
