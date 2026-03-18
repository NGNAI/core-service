package ai.dto.own.response;

import ai.enums.MediaUploadTarget;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaResponseDto {
    UUID id;
    String name;
    String type;
    Long size;
    String minioPath;
    UUID parentId;
    UUID ownerId;
    UUID orgId;
    String accessLevel;
    UUID jobId;
    String ingestionStatus;
    Integer downloadCount;
    Instant createdAt;
    Instant updatedAt;
    MediaUploadTarget target;
}
