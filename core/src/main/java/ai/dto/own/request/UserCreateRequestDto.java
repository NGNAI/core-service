package ai.dto.own.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreateRequestDto {
    String userName;
    String firstName;
    String lastName;
    String password;
    int gender;
    String email;
    String phoneNumber;
    String source;
}
