package ai.dto.own.request;

import java.util.UUID;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationCreateRequestDto {
    @NotBlank(message = InputValidateKey.ORGANIZATION_NAME_CAN_NOT_BE_NULL_OR_EMPTY)
    String name;

    String description;
    
    @NotNull(message = InputValidateKey.PARENT_ORGANIZATION_ID_CAN_NOT_BE_NULL_OR_EMPTY)
    UUID parentId;
}
