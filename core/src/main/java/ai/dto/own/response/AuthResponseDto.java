package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@JsonPropertyOrder({
        "token",
        "user",
})
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthResponseDto {
    String token;
    UserWithOrgResponseDto user;
}
