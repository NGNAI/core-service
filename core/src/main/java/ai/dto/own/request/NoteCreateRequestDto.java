package ai.dto.own.request;

import ai.annotation.EnumValue;
import ai.constant.InputValidateKey;
import ai.enums.NoteSourceType;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteCreateRequestDto {
    String title;

    @NotBlank(message = InputValidateKey.NOTE_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY)
    String content;

    @NotBlank(message = InputValidateKey.NOTE_SOURCE_TYPE_CAN_NOT_BE_NULL_OR_EMPTY)
    @EnumValue(enumClass = NoteSourceType.class, message = InputValidateKey.NOTE_SOURCE_TYPE_INVALID)
    String sourceType;

    UUID topicId;
    UUID noteBookId;
}
