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
 * User APIs cho Quick Prompt Template.
 * <p>
 * User CRUD prompt cá nhân (scope=USER) và xem system prompt (scope=SYSTEM, global, active).
 * Tất cả endpoints đều yêu cầu authenticated (JWT) qua security chain {@code /user/**}.
 */
@Tag(name = "Prompt Template", description = "Prompt template APIs — lưu prompt nhanh dùng cho chat với Topic/NotebookLM")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/prompt-templates")
@RestController
public class PromptTemplateUserController {

    PromptTemplateService promptTemplateService;

    @Operation(summary = "Get prompt types", description = "Lấy danh sách loại prompt khả dụng (TOPIC / NOTEBOOK / BOTH)")
    @GetMapping("/types")
    ResponseEntity<ApiResponseModel<List<PromptType>>> types() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<PromptType>>builder()
                        .message("Get prompt types successfully")
                        .data(Arrays.asList(PromptType.values()))
                        .build());
    }

    @Operation(summary = "List my prompt templates", description = "Lấy danh sách prompt của tôi + system prompt (global, active). Lọc theo promptType / scope / keyword")
    @GetMapping
    ResponseEntity<ApiResponseModel<List<PromptTemplateResponseDto>>> getAll(
            @Valid @ModelAttribute PromptTemplateFilterDto filterDto) {
        CustomPairModel<Long, List<PromptTemplateResponseDto>> result = promptTemplateService.getAllForUser(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<PromptTemplateResponseDto>>builder()
                        .message("Get list prompt templates successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build());
    }

    @Operation(summary = "Get prompt template by id", description = "Xem chi tiết prompt (của mình hoặc system prompt)")
    @GetMapping("/{promptId}")
    ResponseEntity<ApiResponseModel<PromptTemplateResponseDto>> getById(@PathVariable UUID promptId) {
        return ResponseEntity.ok(
                ApiResponseModel.<PromptTemplateResponseDto>builder()
                        .message("Get prompt template successfully")
                        .data(promptTemplateService.getByIdForUser(promptId))
                        .build());
    }

    @Operation(summary = "Create prompt template", description = "Tạo prompt cá nhân của tôi (scope=USER)")
    @PostMapping
    ResponseEntity<ApiResponseModel<PromptTemplateResponseDto>> create(
            @Valid @RequestBody PromptTemplateCreateRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<PromptTemplateResponseDto>builder()
                        .message("Create prompt template successfully")
                        .data(promptTemplateService.create(requestDto))
                        .build());
    }

    @Operation(summary = "Update prompt template", description = "Cập nhật prompt cá nhân của tôi (partial update — field null giữ nguyên)")
    @PutMapping("/{promptId}")
    ResponseEntity<ApiResponseModel<PromptTemplateResponseDto>> update(@PathVariable UUID promptId,
            @Valid @RequestBody PromptTemplateUpdateRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<PromptTemplateResponseDto>builder()
                        .message("Update prompt template successfully")
                        .data(promptTemplateService.update(promptId, requestDto))
                        .build());
    }

    @Operation(summary = "Delete prompt template", description = "Xóa prompt cá nhân của tôi")
    @DeleteMapping("/{promptId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID promptId) {
        promptTemplateService.delete(promptId);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete prompt template successfully")
                        .build());
    }
}
