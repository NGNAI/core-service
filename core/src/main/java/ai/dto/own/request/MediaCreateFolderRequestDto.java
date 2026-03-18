package ai.dto.own.request;

import ai.enums.MediaUploadTarget;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaCreateFolderRequestDto {
    String name;
    UUID parentId;
    UUID ownerId;
    UUID orgId;
    String visibility;
    MediaUploadTarget target;
}
