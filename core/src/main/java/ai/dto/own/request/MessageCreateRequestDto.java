package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageCreateRequestDto {
    @NotBlank(message = InputValidateKey.MESSAGE_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY)
    String content;
    @NotBlank(message = InputValidateKey.MESSAGE_TYPE_CAN_NOT_BE_NULL_OR_EMPTY)
    String type;
//    @NotBlank(message = InputValidateKey.MESSAGE_PARENT_ID_CAN_NOT_BE_NULL_OR_EMPTY)
//    UUID parentId;
//    @NotBlank(message = InputValidateKey.MESSAGE_PARENT_TYPE_CAN_NOT_BE_NULL_OR_EMPTY)
//    @EnumValue(enumClass = MessageParentType.class, message = InputValidateKey.INVALID_MESSAGE_PARENT_VALUE)
//    String parentType;
}
