package ai.controller;

import ai.dto.own.request.ConversationRequestDto;
import ai.service.RagService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/conversation")
@RestController
public class ConversationController {
    RagService ragService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> conversation(@Valid @RequestBody ConversationRequestDto requestDto) throws JsonProcessingException {
        return ragService.chat(null,requestDto);
    }

    @PostMapping(value = "/{topicId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> conversation(@PathVariable UUID topicId, @Valid @RequestBody ConversationRequestDto requestDto) throws JsonProcessingException {
        return ragService.chat(topicId,requestDto);
    }
}
