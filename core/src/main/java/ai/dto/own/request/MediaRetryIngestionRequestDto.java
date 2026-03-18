package ai.dto.own.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaRetryIngestionRequestDto {
    String username;
    String unit;
    String visibility;
}
