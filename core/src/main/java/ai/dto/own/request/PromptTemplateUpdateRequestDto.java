package ai.dto.own.request;

import ai.enums.PromptType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request cập nhật prompt template. Các field bỏ trống (null) sẽ giữ nguyên giá trị hiện tại (partial update).
 * {@code isActive} chỉ có ý nghĩa với admin.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromptTemplateUpdateRequestDto {

    @Schema(description = "Tên hiển thị của prompt")
    String title;

    @Schema(description = "Nội dung prompt")
    String content;

    @Schema(description = "Loại chatbot: TOPIC / NOTEBOOK / BOTH")
    PromptType promptType;

    @Schema(description = "Thứ tự hiển thị")
    Integer displayOrder;

    @Schema(description = "Trạng thái hiệu lực (chỉ admin dùng để bật/tắt prompt)")
    Boolean isActive;
}
