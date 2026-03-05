package ai.dto.own.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserWithOrgResponseDto extends UserResponseDto {
    Set<OrganizationWithUserRoleDto> organizations = new HashSet<>();
}
