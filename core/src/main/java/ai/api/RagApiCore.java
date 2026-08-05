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

    private String prettyPrint(String json) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(json));
        } catch (JsonProcessingException e) {
            return json;
        }
    }
}
