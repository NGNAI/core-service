package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DraftSaveRequestDto {
    @NotBlank(message = InputValidateKey.DRAFT_TYPE_CAN_NOT_BE_NULL_OR_EMPTY)
    String type;

    @NotBlank(message = InputValidateKey.DRAFT_TITLE_CAN_NOT_BE_NULL_OR_EMPTY)
    String title;

    @NotBlank(message = InputValidateKey.DRAFT_DESCRIPTION_CAN_NOT_BE_NULL_OR_EMPTY)
    String detailedDescription;

    @NotBlank(message = InputValidateKey.DRAFT_PRESENTATION_STYLE_CAN_NOT_BE_NULL_OR_EMPTY)
    String presentationStyle;

    @NotBlank(message = InputValidateKey.DRAFT_LANGUAGE_CAN_NOT_BE_NULL_OR_EMPTY)
    String language;

    String tone;
    String targetAudience;
    String outputLength;
    String formatInstruction;
    String additionalInstruction;
    String changeRequest;

    @NotBlank(message = InputValidateKey.DRAFT_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY)
    String generatedContent;
}
