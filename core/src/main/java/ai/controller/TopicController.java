package ai.controller;

import ai.dto.own.request.TopicCreateRequestDto;
import ai.dto.own.request.TopicRenameTitleRequestDto;
import ai.dto.own.request.filter.TopicFilterDto;
import ai.dto.own.response.TopicResponseDto;
import ai.model.ApiResponseModel;
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
@RequestMapping("/prv/topic")
@RestController
public class TopicController {
    TopicService topicService;

    @GetMapping("/{userId}")
    ResponseEntity<ApiResponseModel<List<TopicResponseDto>>> getAllByUserId(int userId, @Valid @ModelAttribute TopicFilterDto filterDto){
        return ResponseEntity.ok(
                ApiResponseModel.<List<TopicResponseDto>>builder()
                        .message("Get list topics successfully")
                        .data(topicService.getAll(userId, filterDto))
                        .build()
        );
    }

    @PostMapping
    ResponseEntity<ApiResponseModel<TopicResponseDto>> create(@Valid @RequestBody TopicCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<TopicResponseDto>builder()
                        .message("Create topic successfully")
                        .data(topicService.create(requestDto))
                        .build()
        );
    }

    @PatchMapping("/{topicId}")
    ResponseEntity<ApiResponseModel<TopicResponseDto>> renameTitle(@Valid @PathVariable int topicId, @RequestBody TopicRenameTitleRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<TopicResponseDto>builder()
                        .message("Rename topic title successfully")
                        .data(topicService.renameTitle(topicId, requestDto))
                        .build()
        );
    }

    @DeleteMapping("/{topicId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable int topicId){
        topicService.delete(topicId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete topic successfully")
                        .build()
        );
    }
}
