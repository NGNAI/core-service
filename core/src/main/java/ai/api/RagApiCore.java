package ai.api;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class RagApiCore {
    WebClient ragWebClient;

    ObjectMapper objectMapper;

    public Flux<String> post(String endPoint, Object body) throws JsonProcessingException {
        String jsonBody = objectMapper.writeValueAsString(body);
        log.info("RAG POST {} request body:\n{}", endPoint, prettyPrint(jsonBody));
        return ragWebClient.post()
                .uri(endPoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnError(WebClientResponseException.class, e ->
                        log.error("RAG POST {} failed with status {}:\n{}",
                                endPoint, e.getStatusCode(), e.getResponseBodyAsString()));
    }

    public String postForString(String endPoint, Object body) throws JsonProcessingException {
        String jsonBody = objectMapper.writeValueAsString(body);
        log.info("RAG POST {} request body:\n{}", endPoint, prettyPrint(jsonBody));
        try {
            return ragWebClient.post()
                    .uri(endPoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("RAG POST {} failed with status {}:\n{}",
                    endPoint, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * Gọi POST đồng bộ (blocking) và parse response về object theo responseType.
     * Dùng cho các endpoint trả về JSON đơn lẻ (không phải SSE), ví dụ trigger source-guide.
     * @param endPoint path bắt đầu bằng "/"
     * @param body đối tượng request sẽ được serialize thành JSON
     * @param responseType class của response DTO
     * @return object đã parse
     */
    public <T> T postForObject(String endPoint, Object body, Class<T> responseType) throws JsonProcessingException {
        String jsonBody = objectMapper.writeValueAsString(body);
        log.info("RAG POST {} request body:\n{}", endPoint, prettyPrint(jsonBody));
        try {
            return ragWebClient.post()
                    .uri(endPoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("RAG POST {} failed with status {}:\n{}",
                    endPoint, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * Gọi GET đồng bộ (blocking) và parse response về object theo responseType.
     * Dùng cho các endpoint trả về JSON đơn lẻ, ví dụ GET source-guide.
     * @param endPoint path bắt đầu bằng "/", có thể chứa URI template (vd ?file_id={fileId}&notebook_id={notebookId})
     * @param responseType class của response DTO
     * @param uriVariables các biến URI template nếu có
     * @return object đã parse
     */
    public <T> T getForObject(String endPoint, Class<T> responseType, Object... uriVariables) {
        try {
            return ragWebClient.get()
                    .uri(endPoint, uriVariables)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("RAG GET {} failed with status {}:\n{}",
                    endPoint, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    private String prettyPrint(String json) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(json));
        } catch (JsonProcessingException e) {
            return json;
        }
    }
}
