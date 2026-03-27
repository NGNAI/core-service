package ai.dto.own.response;

import java.util.UUID;

import ai.enums.DataScope;
import ai.enums.MediaUploadTarget;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaResponseDto extends AuditResponseDto {
    UUID id;
    String name;
    boolean folder;
    String contentType;
    Long fileSize;
    String minioPath;
    UUID parentId;
    UUID ownerId;
    UUID orgId;
    DataScope accessLevel;
    UUID jobId;
    String ingestionStatus;
    MediaUploadTarget target;
}
