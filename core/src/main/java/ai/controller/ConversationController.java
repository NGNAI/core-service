package ai.controller;

import ai.dto.own.request.ConversationRequestDto;
import ai.dto.own.request.ConversationAttachmentUploadRequestDto;
import ai.dto.own.response.AttachmentResponseDto;
import ai.model.ApiResponseModel;
import ai.service.AttachmentService;
import ai.service.RagService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
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
    AttachmentService attachmentService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> conversation(@Valid @RequestBody ConversationRequestDto requestDto) throws JsonProcessingException {
        return ragService.chat(null,requestDto);
    }

    @PostMapping(value = "/{topicId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> conversation(@PathVariable UUID topicId, @Valid @RequestBody ConversationRequestDto requestDto) throws JsonProcessingException {
        return ragService.chat(topicId,requestDto);
    }

    @PostMapping(value = "/{topicId}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseModel<AttachmentResponseDto>> uploadAttachmentToTopic(
            @PathVariable UUID topicId,
            @Valid @ModelAttribute ConversationAttachmentUploadRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<AttachmentResponseDto>builder()
                        .message("Upload attachment to topic successfully")
                        .data(attachmentService.uploadToTopic(topicId, requestDto.getFile()))
                        .build());
    }
}
