package ai.controller;

import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.response.MessageResponseDto;
import ai.dto.own.response.TopicResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.MessageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/prv")
@RestController
public class MessageController {
    MessageService messageService;
    @GetMapping("/topic/{topicId}/message")
    ResponseEntity<ApiResponseModel<List<MessageResponseDto>>> getAllByTopicId(@PathVariable int topicId, @ModelAttribute MessageFilterDto filterDto){
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
