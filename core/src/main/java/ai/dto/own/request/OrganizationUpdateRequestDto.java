package ai.dto.own.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationUpdateRequestDto {
    @NotBlank(message = "ORGANIZATION_NAME_CAN_NOT_BE_NULL_OR_EMPTY")
    String name;
    String description;
    UUID parentId;
}
