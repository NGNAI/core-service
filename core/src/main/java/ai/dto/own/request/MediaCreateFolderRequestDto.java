package ai.dto.own.request;

import ai.enums.MediaUploadTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaCreateFolderRequestDto {
    @NotBlank(message = "MEDIA_NAME_REQUIRED")
    String name;
    UUID parentId;
    @NotNull(message = "MEDIA_OWNER_ID_REQUIRED")
    UUID ownerId;
    @NotNull(message = "MEDIA_ORG_ID_REQUIRED")
    UUID orgId;
    String visibility;
    @NotNull(message = "MEDIA_TARGET_INVALID")
    MediaUploadTarget target;
}
