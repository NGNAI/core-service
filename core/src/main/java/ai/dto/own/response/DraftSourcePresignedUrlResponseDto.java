package ai.dto.own.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class DraftSourcePresignedUrlResponseDto {
    String url;
    Integer expiresInSeconds;
}
