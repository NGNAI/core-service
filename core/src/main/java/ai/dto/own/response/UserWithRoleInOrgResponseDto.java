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
public class UserWithRoleInOrgResponseDto extends UserResponseDto {
    Set<RoleSimplifyResponseDto> roles = new HashSet<>();
}
