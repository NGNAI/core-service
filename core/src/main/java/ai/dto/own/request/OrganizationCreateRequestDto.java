package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationCreateRequestDto {
    @NotBlank(message = InputValidateKey.ORGANIZATION_NAME_CAN_NOT_BE_NULL_OR_EMPTY)
    String name;
    String description;
    @NotBlank(message = InputValidateKey.PARENT_ORGANIZATION_ID_CAN_NOT_BE_NULL_OR_EMPTY)
    UUID parentId;
}
