package ai.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class RagApiCore {
    WebClient ragWebClient;

    ObjectMapper objectMapper;

    public Flux<String> post(String endPoint, Object body) throws JsonProcessingException {
        return ragWebClient.post()
                .uri(endPoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(objectMapper.writeValueAsString(body))
                .retrieve()
                .bodyToFlux(String.class);
    }

    public String postForString(String endPoint, Object body) throws JsonProcessingException {
        return ragWebClient.post()
                .uri(endPoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(body))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

}
