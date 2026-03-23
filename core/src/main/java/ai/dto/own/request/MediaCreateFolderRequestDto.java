package ai.dto.own.request;

import ai.enums.DataScope;
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

    @NotNull(message = "MEDIA_ACCESS_LEVEL_INVALID")
    DataScope accessLevel;

    @NotNull(message = "MEDIA_TARGET_INVALID")
    MediaUploadTarget target;
}
