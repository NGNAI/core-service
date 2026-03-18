package ai.dto.own.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaJobStatusResponseDto {
    UUID mediaId;
    UUID jobId;
    String ingestionStatus;
    String message;
}
