package ai.dto.own.request;

import java.util.Set;

import ai.annotation.EnumValue;
import ai.constant.InputValidateKey;
import ai.enums.RagScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class DraftCreateRequestDto {
    @NotBlank(message = InputValidateKey.DRAFT_TYPE_CAN_NOT_BE_NULL_OR_EMPTY)
    String type;

    @NotEmpty(message = InputValidateKey.RAG_SCOPE_CAN_NOT_BE_NULL_OR_EMPTY)
    Set<
            @EnumValue(enumClass = RagScope.class, message = InputValidateKey.INVALID_RAG_SCOPE_VALUE)
            String> scopes;

    @Schema(description = "Chuẩn định dạng văn bản (format_standard), ví dụ nd30_2020")
    String format;

    @NotBlank(message = InputValidateKey.DRAFT_TITLE_CAN_NOT_BE_NULL_OR_EMPTY)
    String title;

    @NotBlank(message = InputValidateKey.DRAFT_DESCRIPTION_CAN_NOT_BE_NULL_OR_EMPTY)
    String detailedDescription;
}
