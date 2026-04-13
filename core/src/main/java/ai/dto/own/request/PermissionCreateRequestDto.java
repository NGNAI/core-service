package ai.dto.own.request;

import ai.annotation.EnumValue;
import ai.constant.InputValidateKey;
import ai.enums.PermissionAction;
import ai.enums.PermissionResource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionCreateRequestDto {
    @NotBlank(message = InputValidateKey.PERMISSION_NAME_CAN_NOT_BE_NULL_OR_EMPTY)
    String name;
    String description;
    @NotBlank(message = InputValidateKey.PERMISSION_RESOURCE_CAN_NOT_BE_NULL_OR_EMPTY)
    @EnumValue(enumClass = PermissionResource.class,message = InputValidateKey.INVALID_VALUE_PERMISSION_RESOURCE)
    String resource;
    @NotBlank(message = InputValidateKey.PERMISSION_ACTION_CAN_NOT_BE_NULL_OR_EMPTY)
    @EnumValue(enumClass = PermissionAction.class,message = InputValidateKey.INVALID_VALUE_PERMISSION_ACTION)
    String action;
    @EnumValue(enumClass = PermissionResource.class,message = InputValidateKey.INVALID_VALUE_PERMISSION_RESOURCE)
    String targetResource;
}
