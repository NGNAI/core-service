package ai.controller;

import java.util.List;
import java.util.UUID;

import ai.dto.own.request.TopicSourcesAddRequestDto;
import ai.dto.own.response.TopicSourceDownloadData;
import ai.dto.own.response.TopicSourcePresignedUrlResponseDto;
import ai.dto.own.response.TopicSourceResponseDto;
import ai.enums.MessageParentType;
import org.springframework.data.util.Pair;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import ai.dto.own.request.TopicCreateConversationRequestDto;
import ai.dto.own.request.TopicCreateRequestDto;
import ai.dto.own.request.TopicRenameTitleRequestDto;
import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.request.filter.TopicFilterDto;
import ai.dto.own.response.MessageResponseDto;
import ai.dto.own.response.TopicResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.MessageService;
import ai.service.RagService;
import ai.service.TopicSourceService;
import io.swagger.v3.oas.annotations.Hidden;
import ai.service.TopicService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/topics")
@RestController
public class TopicController {
    TopicService topicService;
    MessageService messageService;
    TopicSourceService topicSourceService;
    RagService ragService;

    @GetMapping()
    ResponseEntity<ApiResponseModel<List<TopicResponseDto>>> getAllByUserId(@Valid @ModelAttribute TopicFilterDto filterDto){
        CustomPairModel<Long, List<TopicResponseDto>> result = topicService.getAll(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<TopicResponseDto>>builder()
                        .message("Get list topics successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @PostMapping()
    ResponseEntity<ApiResponseModel<TopicResponseDto>> create(@Valid @RequestBody TopicCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<TopicResponseDto>builder()
                        .message("Create topic successfully")
                        .data(topicService.create(requestDto))
                        .build()
        );
    }

    @PatchMapping("/{topicId}")
    ResponseEntity<ApiResponseModel<TopicResponseDto>> renameTitle(@PathVariable UUID topicId, @Valid @RequestBody TopicRenameTitleRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<TopicResponseDto>builder()
                        .message("Rename topic title successfully")
                        .data(topicService.renameTitle(topicId, requestDto))
                        .build()
        );
    }

    @DeleteMapping("/{topicId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID topicId){
        topicService.delete(topicId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete topic successfully")
                        .build()
        );
    }

    @Hidden
    @GetMapping("/{topicId}/sources")
    ResponseEntity<ApiResponseModel<List<TopicSourceResponseDto>>> getSources(@PathVariable UUID topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pair<Long, List<TopicSourceResponseDto>> result = topicSourceService.getSources(topicId, page, size);
        return ResponseEntity.ok(
                ApiResponseModel.<List<TopicSourceResponseDto>>builder()
                        .message("Get list topic sources successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @Hidden
    @GetMapping("/{topicId}/sources/{sourceId}/download")
    ResponseEntity<byte[]> downloadSource(@PathVariable UUID topicId, @PathVariable UUID sourceId) {
        TopicSourceDownloadData fileData = topicSourceService.downloadSource(topicId, sourceId);

        HttpHeaders headers = new HttpHeaders();
        String contentType = (fileData.contentType() == null || fileData.contentType().isBlank())
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
            : fileData.contentType();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileData.fileName()).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileData.bytes());
    }

    @Hidden
    @GetMapping("/{topicId}/sources/{sourceId}/download-url")
    ResponseEntity<ApiResponseModel<TopicSourcePresignedUrlResponseDto>> getDownloadUrl(
            @PathVariable UUID topicId,
            @PathVariable UUID sourceId,
            @RequestParam(required = false) Integer expiresInSeconds) {
        TopicSourcePresignedUrlResponseDto downloadUrl = topicSourceService.getSourceDownloadUrl(topicId, sourceId, expiresInSeconds);
        return ResponseEntity.ok(
                ApiResponseModel.<TopicSourcePresignedUrlResponseDto>builder()
                        .message("Get source download URL successfully")
                        .data(downloadUrl)
                        .build()
        );
    }

    @Hidden
    @PostMapping(value = "/{topicId}/sources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponseModel<List<TopicSourceResponseDto>>> addSources(@PathVariable UUID topicId,
            @Valid @ModelAttribute TopicSourcesAddRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<List<TopicSourceResponseDto>>builder()
                        .message("Add source to topic successfully")
                        .data(topicSourceService.uploadSources(topicId, requestDto))
                        .build()
        );
    }

    @Hidden
    @DeleteMapping("/{topicId}/sources/{sourceId}")
    ResponseEntity<ApiResponseModel<Void>> removeSource(@PathVariable UUID topicId, @PathVariable UUID sourceId) {
        topicSourceService.removeSource(topicId, sourceId);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Remove source from topic successfully")
                        .build()
        );
    }

    @GetMapping("/{topicId}/messages")
    ResponseEntity<ApiResponseModel<List<MessageResponseDto>>> getMessageByTopicId(@PathVariable UUID topicId,@Valid @ModelAttribute MessageFilterDto filterDto){
        CustomPairModel<Long, List<MessageResponseDto>> result = messageService.getAll(topicId, MessageParentType.TOPIC, filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<MessageResponseDto>>builder()
                        .message("Get list message of topic successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @PostMapping(value = "/{topicId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> postMessageByTopicIdFlux(
            @PathVariable UUID topicId,
            @Valid @ModelAttribute TopicCreateConversationRequestDto requestDto) throws JsonProcessingException {
        if (requestDto.getFiles() != null && requestDto.getFiles().length > 0) {
            TopicSourcesAddRequestDto sourceRequest = new TopicSourcesAddRequestDto();
            sourceRequest.setFiles(requestDto.getFiles());
            List<TopicSourceResponseDto> uploadedSources = topicSourceService.uploadSources(topicId, sourceRequest);
            uploadedSources.forEach(uploadedSource -> messageService.createAttachmentMessage(topicId, uploadedSource));
        }

        return ragService.chatTopic(topicId, requestDto);
    }
}
