package ai.controller;

import ai.dto.own.request.TopicCreateRequestDto;
import ai.dto.own.request.TopicRenameTitleRequestDto;
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

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/prv")
@RestController
public class TopicController {
    TopicService topicService;

    @GetMapping("user/{userId}/topic")
    ResponseEntity<ApiResponseModel<List<TopicResponseDto>>> getAllByUserId(@PathVariable int userId, @ModelAttribute TopicFilterDto filterDto){
        CustomPairModel<Long, List<TopicResponseDto>> result = topicService.getAll(userId, filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<TopicResponseDto>>builder()
                        .message("Get list topics successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @PostMapping("/topic")
    ResponseEntity<ApiResponseModel<TopicResponseDto>> create(@Valid @RequestBody TopicCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<TopicResponseDto>builder()
                        .message("Create topic successfully")
                        .data(topicService.create(requestDto))
                        .build()
        );
    }

    @PatchMapping("/topic/{topicId}")
    ResponseEntity<ApiResponseModel<TopicResponseDto>> renameTitle(@PathVariable int topicId,@Valid @RequestBody TopicRenameTitleRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<TopicResponseDto>builder()
                        .message("Rename topic title successfully")
                        .data(topicService.renameTitle(topicId, requestDto))
                        .build()
        );
    }

    @DeleteMapping("/topic/{topicId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable int topicId){
        topicService.delete(topicId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete topic successfully")
                        .build()
        );
    }
}
