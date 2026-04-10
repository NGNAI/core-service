package ai.dto.own.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DrivePresignedUrlResponseDto {
    String url;
    int expiresInSeconds;
}
