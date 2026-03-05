package ai.dto.own.response;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponseDto {
    int id;
    String userName;
    String firstName;
    String lastName;
    int gender;
    String email;
    String phoneNumber;
    String source;
}
