package ai.service.api;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import ai.api.RagApiCore;
import ai.dto.outer.rag.request.RagCompletionRequestDto;
import ai.dto.outer.rag.request.RagDraftCreateRequestDto;
import ai.dto.outer.rag.request.RagDraftReviseRequestDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class RagApiService {
    RagApiCore apiCore;

    public Flux<String> topicChat(RagCompletionRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/v2/chat/completions", requestDto);
    }

    public Flux<String> noteBookChat(RagCompletionRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/v2/notebook/chat/completions", requestDto);
    }

    public Flux<String> draftCreate(RagDraftCreateRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/v2/draft/create", requestDto);
    }

    public Flux<String> draftRevise(RagDraftReviseRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/v2/draft/revise", requestDto);
    }

    public String general(RagCompletionRequestDto requestDto) throws JsonProcessingException {
        return apiCore.postForString("/v2/generate/chat/completions_simple", requestDto);
    }
}
