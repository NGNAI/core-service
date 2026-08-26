package ai.controller.user;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.request.FeedbackCreateRequestDto;
import ai.dto.own.request.FeedbackUpdateRequestDto;
import ai.dto.own.request.filter.FeedbackFilterDto;
import ai.dto.own.response.FeedbackResponseDto;
import ai.enums.FeedbackStatus;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Tag(name = "Feedback", description = "Hộp thư góp ý và phản hồi của người dùng")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/feedbacks")
@RestController
public class FeedbackUserController {
    FeedbackService feedbackService;

    @Operation(summary = "Get my feedbacks", description = "Lấy danh sách góp ý của người dùng hiện tại với bộ lọc và phân trang")
    @GetMapping
    ResponseEntity<ApiResponseModel<List<FeedbackResponseDto>>> getAll(@Valid @ModelAttribute FeedbackFilterDto filterDto) {
        CustomPairModel<Long, List<FeedbackResponseDto>> result = feedbackService.getAllForCurrentUser(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<FeedbackResponseDto>>builder()
                        .message("Get list feedbacks successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build());
    }

    @Operation(summary = "Get my feedback by ID", description = "Lấy chi tiết một góp ý của người dùng hiện tại (kiểm tra ownership)")
    @GetMapping("/{id}")
    ResponseEntity<ApiResponseModel<FeedbackResponseDto>> getById(@PathVariable UUID id) {
        FeedbackResponseDto dto = feedbackService.getFeedbackOfUser(id);
        return ResponseEntity.ok(
                ApiResponseModel.<FeedbackResponseDto>builder()
                        .message("Get feedback successfully")
                        .data(dto)
                        .build());
    }

    @Operation(summary = "Create feedback", description = "Gửi góp ý/phản hồi mới")
    @PostMapping
    ResponseEntity<ApiResponseModel<FeedbackResponseDto>> create(@Valid @RequestBody FeedbackCreateRequestDto requestDto) {
        FeedbackResponseDto dto = feedbackService.createFeedback(requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<FeedbackResponseDto>builder()
                        .message("Create feedback successfully")
                        .data(dto)
                        .build());
    }

    @Operation(summary = "Update my feedback", description = "Cập nhật góp ý của người dùng hiện tại (kiểm tra ownership)")
    @PutMapping("/{id}")
    ResponseEntity<ApiResponseModel<FeedbackResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody FeedbackUpdateRequestDto requestDto) {
        FeedbackResponseDto dto = feedbackService.updateFeedback(id, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<FeedbackResponseDto>builder()
                        .message("Update feedback successfully")
                        .data(dto)
                        .build());
    }

    @Operation(summary = "Delete my feedback", description = "Xoá góp ý của người dùng hiện tại (kiểm tra ownership)")
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete feedback successfully")
                        .build());
    }

    @Operation(summary = "Get feedback statuses", description = "Retrieve all feedback status values")
    @GetMapping("/statuses")
    ResponseEntity<ApiResponseModel<List<FeedbackStatus>>> statuses() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<FeedbackStatus>>builder()
                        .message("Get feedback statuses successfully")
                        .data(Arrays.asList(FeedbackStatus.values()))
                        .build());
    }
}
