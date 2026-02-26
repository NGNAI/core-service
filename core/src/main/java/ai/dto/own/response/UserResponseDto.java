package ai.dto.own.response;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponseDto {
    String id;
    String userName;
    String firstName;
    String lastName;
    String password;
    int gender;
    String email;
    String phoneNumber;
    String source;
}
