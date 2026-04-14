package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationSelectRequestDto {
    @NotNull(message = InputValidateKey.ORGANIZATION_ID_CAN_NOT_BE_NULL_OR_EMPTY)
    UUID orgId;
}
