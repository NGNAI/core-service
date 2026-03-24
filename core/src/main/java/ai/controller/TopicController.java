package ai.controller;

import ai.dto.own.request.TopicCreateRequestDto;
import ai.dto.own.request.TopicRenameTitleRequestDto;
import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.request.filter.TopicFilterDto;
import ai.dto.own.response.MessageResponseDto;
import ai.dto.own.response.TopicResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.MessageService;
import ai.service.TopicService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/topics")
@RestController
public class TopicController {
    TopicService topicService;
    MessageService messageService;

    @GetMapping()
    ResponseEntity<ApiResponseModel<List<TopicResponseDto>>> getAllByUserId(@ModelAttribute TopicFilterDto filterDto){
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

    @GetMapping("/{topicId}/messages")
    ResponseEntity<ApiResponseModel<List<MessageResponseDto>>> getMessageByTopicId(@PathVariable UUID topicId, @ModelAttribute MessageFilterDto filterDto){
        CustomPairModel<Long, List<MessageResponseDto>> result = messageService.getAll(topicId, filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<MessageResponseDto>>builder()
                        .message("Get list message of topic successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }
}
