package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteCreateByTopicRequestDto {
    String title;

    @NotBlank(message = InputValidateKey.NOTE_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY)
    String content;

    @NotBlank(message = InputValidateKey.NOTE_TOPIC_ID_CAN_NOT_BE_NULL_OR_EMPTY)
    String topicId;
}
