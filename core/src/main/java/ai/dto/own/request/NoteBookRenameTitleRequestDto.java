package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteBookRenameTitleRequestDto {
    @NotBlank(message = InputValidateKey.TOPIC_TITLE_CAN_NOT_BE_NULL_OR_EMPTY)
    String title;
}
