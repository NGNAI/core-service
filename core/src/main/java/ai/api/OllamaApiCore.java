package ai.api;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.AppProperties;
import ai.dto.outer.ollama.OllamaChatRequestDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Transport layer cho native Ollama API.
 *
 * <p>Tách riêng khỏi {@link RagApiCore} vì:
 * <ul>
 *   <li>Dùng host/model khác (Ollama local thay vì RAG service).</li>
 *   <li>Timeout rất ngắn (autocomplete) thay vì 360s của RAG.</li>
 *   <li>Request/response shape khác (native API, không phải OpenAI-compatible).</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class OllamaApiCore {

    /** Endpoint chat của native Ollama API. */
    private static final String CHAT_ENDPOINT = "/api/chat";

    /** Timeout mặc định (ms) khi không cấu hình — dùng cho {@code block(...)}. */
    private static final long DEFAULT_READ_TIMEOUT_MS = 15_000L;

    WebClient ollamaWebClient;

    ObjectMapper objectMapper;

    AppProperties appProperties;

    /**
     * Gọi {@code POST /api/chat} đồng bộ và trả về raw response body.
     *
     * <p>Dùng {@code block(Duration)} với đúng read timeout thay vì chỉ dựa vào
     * {@code responseTimeout} của HttpClient: {@code responseTimeout} của Reactor Netty
     * tính theo từng lần đọc (read operation), còn autocomplete cần hard-cap
     * tổng thời gian xử lý của một request.
     *
     * @param requestDto request body đã dựng sẵn
     * @return raw JSON response từ Ollama
     */
    public String postChat(OllamaChatRequestDto requestDto) throws JsonProcessingException {
        String jsonBody = objectMapper.writeValueAsString(requestDto);
        log.debug("Ollama POST {} request body: {}", CHAT_ENDPOINT, jsonBody);

        try {
            return ollamaWebClient.post()
                    .uri(CHAT_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofMillis(readTimeoutMs()));
        } catch (WebClientResponseException e) {
            log.error("Ollama POST {} failed with status {}:\n{}",
                    CHAT_ENDPOINT, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    private long readTimeoutMs() {
        Long configured = appProperties.getSuggestion() != null
                ? appProperties.getSuggestion().getReadTimeoutMs()
                : null;
        return (configured != null && configured > 0) ? configured : DEFAULT_READ_TIMEOUT_MS;
    }
}
