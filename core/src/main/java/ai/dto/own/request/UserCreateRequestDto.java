package ai.dto.own.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreateRequestDto {
    String userName;
    String fullName;
    String password;
    String email;
    String source;
    Set<String> roles;
}
