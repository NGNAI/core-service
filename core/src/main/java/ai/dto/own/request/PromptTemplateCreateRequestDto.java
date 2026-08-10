package ai.dto.own.request;

import ai.enums.PromptType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request tạo mới prompt template (dùng cho cả user tạo prompt cá nhân và admin tạo system prompt).
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromptTemplateCreateRequestDto {

    @Schema(description = "Tên hiển thị của prompt", example = "Tóm tắt nội dung")
    @NotBlank(message = "PROMPT_TEMPLATE_TITLE_CAN_NOT_BE_NULL_OR_EMPTY")
    String title;

    @Schema(description = "Nội dung prompt — câu input nhanh khi chat", example = "Hãy tóm tắt nội dung chính của tài liệu thành các ý ngắn gọn.")
    @NotBlank(message = "PROMPT_TEMPLATE_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY")
    String content;

    @Schema(description = "Loại chatbot mà prompt phục vụ: TOPIC / NOTEBOOK / BOTH", example = "TOPIC")
    @NotNull(message = "PROMPT_TEMPLATE_TYPE_CAN_NOT_BE_NULL_OR_EMPTY")
    PromptType promptType;

    @Schema(description = "Thứ tự hiển thị (nhỏ trước, lớn sau)", example = "1")
    @Builder.Default
    Integer displayOrder = 0;
}
