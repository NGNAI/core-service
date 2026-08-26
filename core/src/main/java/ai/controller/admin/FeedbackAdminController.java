package ai.controller.admin;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.request.FeedbackRespondRequestDto;
import ai.dto.own.request.FeedbackStatusUpdateRequestDto;
import ai.dto.own.request.filter.FeedbackFilterDto;
import ai.dto.own.response.FeedbackResponseDto;
import ai.enums.FeedbackStatus;
import ai.model.ApiResponseModel;
import ai.security.AdminAccessGuard;
import ai.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/feedbacks")
@Tag(name = "Feedback Admin", description = "Admin APIs for managing feedback and user suggestions")
@RestController
public class FeedbackAdminController {

    FeedbackService feedbackService;
    AdminAccessGuard adminAccessGuard;

    @Operation(summary = "Check access", description = "Kiểm tra token hiện tại có quyền truy cập Feedback admin APIs (dựa trên danh sách username được phép cấu hình trong hệ thống)")
    @GetMapping("/access")
    ResponseEntity<ApiResponseModel<Boolean>> checkAccess() {
        return ResponseEntity.ok(
                ApiResponseModel.<Boolean>builder()
                        .message("Check access successfully")
                        .data(adminAccessGuard.isAllowed())
                        .build()
        );
    }

    @Operation(summary = "Get feedback by ID", description = "Retrieve a feedback by its ID (admin only, no ownership check)")
    @GetMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "Get feedback successfully",
                 content = @Content(schema = @Schema(implementation = FeedbackResponseDto.class)))
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<FeedbackResponseDto>> getById(@PathVariable UUID id) {
        FeedbackResponseDto dto = feedbackService.getFeedbackMapper()
                .entityToResponseDto(feedbackService.getEntityByIdShared(id));
        return ResponseEntity.ok(
                ApiResponseModel.<FeedbackResponseDto>builder()
                        .message("Get feedback successfully")
                        .data(dto)
                        .build()
        );
    }

    @Operation(summary = "Get all feedbacks", description = "Retrieve all feedbacks with pagination and filters (admin only)")
    @GetMapping
    @ApiResponse(responseCode = "200", description = "Get feedbacks successfully",
                 content = @Content(schema = @Schema(implementation = FeedbackResponseDto.class)))
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<Page<FeedbackResponseDto>>> getAll(@Valid FeedbackFilterDto filterDto) {
        Page<FeedbackResponseDto> page = feedbackService.getAllFeedbacks(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Page<FeedbackResponseDto>>builder()
                        .message("Get feedbacks successfully")
                        .data(page)
                        .build()
        );
    }

    @Operation(summary = "Delete feedback", description = "Delete a feedback by ID (admin only, no ownership check)")
    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "Delete feedback successfully",
                 content = @Content(schema = @Schema(implementation = Void.class)))
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID id) {
        feedbackService.deleteFeedbackShared(id);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete feedback successfully")
                        .build()
        );
    }

    @Operation(summary = "Respond to feedback", description = "Respond to a feedback (admin only)")
    @PostMapping("/{id}/respond")
    @ApiResponse(responseCode = "200", description = "Respond to feedback successfully",
                 content = @Content(schema = @Schema(implementation = FeedbackResponseDto.class)))
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<FeedbackResponseDto>> respond(
            @PathVariable UUID id,
            @Valid @RequestBody FeedbackRespondRequestDto requestDto) {
        FeedbackResponseDto dto = feedbackService.respondFeedback(id, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<FeedbackResponseDto>builder()
                        .message("Respond to feedback successfully")
                        .data(dto)
                        .build()
        );
    }

    @Operation(summary = "Update feedback status", description = "Update feedback status (admin only)")
    @PutMapping("/{id}/status")
    @ApiResponse(responseCode = "200", description = "Update feedback status successfully",
                 content = @Content(schema = @Schema(implementation = FeedbackResponseDto.class)))
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<FeedbackResponseDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody FeedbackStatusUpdateRequestDto requestDto) {
        FeedbackResponseDto dto = feedbackService.updateStatus(id, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<FeedbackResponseDto>builder()
                        .message("Update feedback status successfully")
                        .data(dto)
                        .build()
        );
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
