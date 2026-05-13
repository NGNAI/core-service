package ai.dto.own.request;

import ai.annotation.EnumValue;
import ai.constant.InputValidateKey;
import ai.enums.NoteSourceBy;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteCreateByNoteBookRequestDto {
    String title;

    @NotBlank(message = InputValidateKey.NOTE_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY)
    String content;

    @NotBlank(message = InputValidateKey.NOTE_SOURCE_BY_CAN_NOT_BE_NULL_OR_EMPTY)
    @EnumValue(enumClass = NoteSourceBy.class, message = InputValidateKey.NOTE_SOURCE_BY_INVALID)
    String sourceBy;

    @NotBlank(message = InputValidateKey.NOTE_NOTEBOOK_ID_CAN_NOT_BE_NULL_OR_EMPTY)
    String noteBookId;
}
