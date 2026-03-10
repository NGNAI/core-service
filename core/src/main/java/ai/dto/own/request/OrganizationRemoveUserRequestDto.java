package ai.dto.own.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationRemoveUserRequestDto {
    @NotEmpty(message = "USER_IDS_CAN_NOT_BE_NULL_OR_EMPTY")
    Set<Integer> userIds;
}
