package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionUpdateRequestDto {
    @NotBlank(message = InputValidateKey.PERMISSION_NAME_CAN_NOT_BE_NULL_OR_EMPTY)
    String name;
    String description;
    @NotBlank(message = InputValidateKey.PERMISSION_RESOURCE_CAN_NOT_BE_NULL_OR_EMPTY)
    String resource;
    @NotBlank(message = InputValidateKey.PERMISSION_ACTION_CAN_NOT_BE_NULL_OR_EMPTY)
    String action;
    String targetResource;
}
