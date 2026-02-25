package ai.dto.own.response;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponseDto {
    String userId;
    String userName;
    String fullName;
    String email;
    String source;
}
