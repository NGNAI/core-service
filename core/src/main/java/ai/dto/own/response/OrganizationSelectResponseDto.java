package ai.dto.own.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.FieldDefaults;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationSelectResponseDto {
    String token;
    OrganizationWithUserRoleDto organization;
}
