package ai.dto.own.request;

import ai.enums.MediaUploadTarget;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaUploadRequestDto {
    @NotNull(message = "MEDIA_FILE_REQUIRED")
    MultipartFile file;
    UUID parentId;
    @NotNull(message = "MEDIA_OWNER_ID_REQUIRED")
    UUID ownerId;
    @NotNull(message = "MEDIA_ORG_ID_REQUIRED")
    UUID orgId;
    String visibility;
    String username;
    String unit;
    @NotNull(message = "MEDIA_TARGET_INVALID")
    MediaUploadTarget target;
}
