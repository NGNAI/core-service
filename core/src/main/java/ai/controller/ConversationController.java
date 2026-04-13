package ai.controller;

import ai.dto.own.request.ConversationRequestDto;
import ai.dto.own.request.ConversationWithAttachmentRequestDto;
import ai.service.AttachmentService;
import ai.service.RagService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/conversation")
@RestController
public class ConversationController {
    RagService ragService;
    AttachmentService attachmentService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> conversation(@Valid @RequestBody ConversationRequestDto requestDto) throws JsonProcessingException {
        return ragService.chat(null, requestDto);
    }

    @PostMapping(value = "/{topicId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> conversation(
            @PathVariable UUID topicId,
            @Valid @ModelAttribute ConversationWithAttachmentRequestDto requestDto) throws JsonProcessingException {
        if (requestDto.getFiles() != null) {
            for (MultipartFile file : requestDto.getFiles()) {
                if (file != null && !file.isEmpty()) {
                    attachmentService.uploadToTopic(topicId, file);
                }
            }
        }

        ConversationRequestDto conversationRequest = new ConversationRequestDto();
        conversationRequest.setMessage(requestDto.getMessage());
        return ragService.chat(topicId, conversationRequest);
    }
}
