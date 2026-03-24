package ai.dto.own.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationCreateRequestDto {
    @NotBlank(message = "ORGANIZATION_NAME_CAN_NOT_BE_NULL_OR_EMPTY")
    String name;
    String description;
    UUID parentId;
}
