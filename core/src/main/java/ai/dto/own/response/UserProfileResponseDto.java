package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "userName",
        "firstName",
        "lastName",
        "gender",
        "email",
        "phoneNumber",
        "organization",
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResponseDto {
    String userName;
    String firstName;
    String lastName;
    int gender;
    String email;
    String phoneNumber;
    OrganizationWithUserRoleDto organization;
}
