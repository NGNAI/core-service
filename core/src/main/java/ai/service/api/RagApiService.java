package ai.service.api;

import ai.api.RagApiCore;
import ai.dto.outer.rag.request.RagCompletionRequestDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class RagApiService {
    RagApiCore apiCore;

    public Flux<String> completions(RagCompletionRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/v1/chat/completions", requestDto);
    }
}
