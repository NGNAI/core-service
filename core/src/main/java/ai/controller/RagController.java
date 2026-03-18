package ai.controller;

import ai.dto.outer.rag.request.RagCompletionRequestDto;
import ai.service.RagApiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/pub/test")
@RestController
public class RagController {
    RagApiService ragApiService;

    @CrossOrigin(origins = "*")
    @PostMapping(value = "/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody RagCompletionRequestDto requestDto) throws JsonProcessingException {
        return ragApiService.completions(requestDto);
    }
}
