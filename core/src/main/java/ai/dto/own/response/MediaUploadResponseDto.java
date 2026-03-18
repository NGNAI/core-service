package ai.dto.own.response;

import ai.enums.MediaUploadTarget;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaUploadResponseDto {
    UUID mediaId;
    String minioPath;
    MediaUploadTarget target;
    String ingestionStatus;
    UUID jobId;
}
