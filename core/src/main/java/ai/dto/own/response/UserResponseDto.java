package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@JsonPropertyOrder({
        "id",
        "userName",
        "firstName",
        "lastName",
        "gender",
        "email",
        "phoneNumber",
        "source",
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponseDto extends AuditResponseDto{
    int id;
    String userName;
    String firstName;
    String lastName;
    int gender;
    String email;
    String phoneNumber;
    String source;
    boolean active;
    String lastLogin;
}
