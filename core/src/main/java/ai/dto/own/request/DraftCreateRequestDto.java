package ai.dto.own.request;

import ai.constant.InputValidateKey;
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
public class DraftCreateRequestDto {
    @NotBlank(message = InputValidateKey.DRAFT_TYPE_CAN_NOT_BE_NULL_OR_EMPTY)
    String type;

    @NotBlank(message = InputValidateKey.DRAFT_TITLE_CAN_NOT_BE_NULL_OR_EMPTY)
    String title;

    @NotBlank(message = InputValidateKey.DRAFT_DESCRIPTION_CAN_NOT_BE_NULL_OR_EMPTY)
    String detailedDescription;
}
