package ai.controller;

import ai.dto.outer.rag.request.RagCompletionRequestDto;
import ai.dto.own.request.ConversationRequestDto;
import ai.service.ConversationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/prv/conversation")
@RestController
public class ConversationController {
    ConversationService ragService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> conversation(@RequestBody ConversationRequestDto requestDto) throws JsonProcessingException {
        return ragService.chat(null,requestDto);
    }

    @PostMapping(value = "/{topicId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> conversation(@PathVariable int topicId,@RequestBody ConversationRequestDto requestDto) throws JsonProcessingException {
        return ragService.chat(topicId,requestDto);
    }
}
