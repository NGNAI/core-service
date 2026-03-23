package ai.dto.own.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationSelectResponseDto {
    String token;
    OrganizationWithUserRoleDto organization;
}
