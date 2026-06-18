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
public class DraftSaveVersionRequestDto {
    String detailedDescription;
    String changeRequest;

    @NotBlank(message = InputValidateKey.DRAFT_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY)
    String currentDraftContent;
}
