package ai.dto.own.request;

import ai.annotation.EnumValue;
import ai.constant.InputValidateKey;
import ai.enums.MessageFeedbackType;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageFeedbackRequestDto {
    @NotBlank(message = InputValidateKey.MESSAGE_FEEDBACK_CAN_NOT_BE_NULL_OR_EMPTY)
    @EnumValue(enumClass = MessageFeedbackType.class, message = InputValidateKey.INVALID_MESSAGE_FEEDBACK_VALUE)
    String feedback;
}
