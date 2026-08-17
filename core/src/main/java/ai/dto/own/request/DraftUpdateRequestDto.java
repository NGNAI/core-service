package ai.dto.own.request;

import ai.constant.InputValidateKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class DraftUpdateRequestDto {
    @NotBlank(message = InputValidateKey.DRAFT_TITLE_CAN_NOT_BE_NULL_OR_EMPTY)
    @Schema(description = "Tiêu đề bản soạn thảo")
    String title;
}
