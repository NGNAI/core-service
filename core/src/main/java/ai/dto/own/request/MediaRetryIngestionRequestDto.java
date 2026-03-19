package ai.dto.own.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaRetryIngestionRequestDto {
    @NotBlank(message = "MEDIA_USERNAME_REQUIRED")
    String username;
    @NotBlank(message = "MEDIA_UNIT_REQUIRED")
    String unit;
    String visibility;
}
