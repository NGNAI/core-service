package ai.dto.own.request;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import ai.enums.DataScope;
import ai.enums.MediaUploadTarget;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaUploadRequestDto {
    @NotNull(message = "MEDIA_FILE_REQUIRED")
    MultipartFile file;
    
    UUID folderId;

    @NotNull(message = "MEDIA_ACCESS_LEVEL_INVALID")
    DataScope accessLevel;

    @NotNull(message = "MEDIA_TARGET_INVALID")
    MediaUploadTarget target;
}
