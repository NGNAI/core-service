package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;
import lombok.experimental.FieldDefaults;

@JsonPropertyOrder({
        "token",
        "organization",
})
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationSelectResponseDto {
    String token;
    OrganizationWithUserRoleDto organization;
}
