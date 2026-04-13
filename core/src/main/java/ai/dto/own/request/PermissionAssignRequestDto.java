package ai.dto.own.request;

import ai.annotation.EnumValue;
import ai.constant.InputValidateKey;
import ai.enums.PermissionAction;
import ai.enums.PermissionScope;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionAssignRequestDto {
    @NotNull(message = InputValidateKey.ASSIGN_PERMISSION_ID_CAN_NOT_BE_NULL)
    UUID id;
    @NotNull(message = InputValidateKey.ASSIGN_PERMISSION_SCOPE_CAN_NOT_BE_NULL)
    @EnumValue(enumClass = PermissionScope.class,message = InputValidateKey.INVALID_VALUE_PERMISSION_SCOPE)
    String scope;
}
