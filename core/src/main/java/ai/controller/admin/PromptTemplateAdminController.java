package ai.controller.admin;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.request.PromptTemplateCreateRequestDto;
import ai.dto.own.request.PromptTemplateUpdateRequestDto;
import ai.dto.own.request.filter.PromptTemplateFilterDto;
import ai.dto.own.response.PromptTemplateResponseDto;
import ai.enums.PromptType;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.PromptTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Admin APIs cho Quick Prompt Template.
 * <p>
 * Admin quản lý system prompt (global) và có thể xem/sửa/xóa tất cả user prompt trong org của mình.
 * Bảo vệ bằng {@code @adminAccessGuard.isAllowed()} (whitelist username) giống System Setting.
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/prompt-templates")
@Tag(name = "Prompt Template Admin", description = "Admin APIs for managing prompt templates (system + user)")
@RestController
public class PromptTemplateAdminController {

    PromptTemplateService promptTemplateService;

    @Operation(summary = "Get prompt types", description = "Lấy danh sách loại prompt khả dụng (TOPIC / NOTEBOOK / BOTH)")
    @GetMapping("/types")
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<List<PromptType>>> types() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<PromptType>>builder()
                        .message("Get prompt types successfully")
                        .data(Arrays.asList(PromptType.values()))
                        .build());
    }

    @Operation(summary = "List all prompt templates", description = "List tất cả prompt: system prompt (global) + user prompt trong org của admin. Có filter theo promptType / scope / isActive / keyword")
    @GetMapping
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<List<PromptTemplateResponseDto>>> getAll(
            @Valid @ModelAttribute PromptTemplateFilterDto filterDto) {
        CustomPairModel<Long, List<PromptTemplateResponseDto>> result = promptTemplateService.getAllForAdmin(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<PromptTemplateResponseDto>>builder()
                        .message("Get list prompt templates successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build());
    }

    @Operation(summary = "Get prompt template by id", description = "Xem chi tiết một prompt template (system hoặc user prompt trong org)")
    @GetMapping("/{promptId}")
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<PromptTemplateResponseDto>> getById(@PathVariable UUID promptId) {
        return ResponseEntity.ok(
                ApiResponseModel.<PromptTemplateResponseDto>builder()
                        .message("Get prompt template successfully")
                        .data(promptTemplateService.getByIdForAdmin(promptId))
                        .build());
    }

    @Operation(summary = "Create system prompt template", description = "Tạo system prompt dùng chung cho tất cả org (global)")
    @PostMapping
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<PromptTemplateResponseDto>> create(
            @Valid @RequestBody PromptTemplateCreateRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<PromptTemplateResponseDto>builder()
                        .message("Create system prompt template successfully")
                        .data(promptTemplateService.createSystem(requestDto))
                        .build());
    }

    @Operation(summary = "Update prompt template", description = "Cập nhật prompt bất kỳ (system hoặc user prompt trong org)")
    @PutMapping("/{promptId}")
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<PromptTemplateResponseDto>> update(@PathVariable UUID promptId,
            @Valid @RequestBody PromptTemplateUpdateRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<PromptTemplateResponseDto>builder()
                        .message("Update prompt template successfully")
                        .data(promptTemplateService.updateForAdmin(promptId, requestDto))
                        .build());
    }

    @Operation(summary = "Delete prompt template", description = "Xóa prompt bất kỳ (system hoặc user prompt trong org)")
    @DeleteMapping("/{promptId}")
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID promptId) {
        promptTemplateService.deleteForAdmin(promptId);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete prompt template successfully")
                        .build());
    }
}
