package ai.service.api;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.api.RagApiCore;
import ai.dto.outer.rag.request.RagCompletionRequestDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class RagApiService {
    RagApiCore apiCore;
    ObjectMapper objectMapper;

    public Flux<String> topicChat(RagCompletionRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/rag/v2/chat/completions", requestDto);
    }

    public Flux<String> noteBookChat(RagCompletionRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/notebook/v2/chat/completions", requestDto);
    }

    public Flux<String> draftChat(RagCompletionRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/rag/v1/chat/completions", requestDto);
    }

    public String general(RagCompletionRequestDto requestDto) throws JsonProcessingException {
        String response = apiCore.postForString("/rag/v1/chat/completions_simple", requestDto);

        if (response == null || response.isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(response);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");

        if (contentNode.isTextual()) {
            return contentNode.asText();
        }

        if (contentNode.isArray()) {
            StringBuilder content = new StringBuilder();
            for (JsonNode part : contentNode) {
                if (part.isTextual()) {
                    content.append(part.asText());
                } else if (part.isObject()) {
                    JsonNode textPart = part.path("text");
                    if (textPart.isTextual()) {
                        content.append(textPart.asText());
                    }
                }
            }
            return content.length() > 0 ? content.toString() : null;
        }

        return null;
    }
}
