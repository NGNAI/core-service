package ai.dto.own.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationWithUserRoleDto extends OrganizationResponseDto{
    Set<RoleSimplifyResponseDto> roles = new HashSet<>();
}
