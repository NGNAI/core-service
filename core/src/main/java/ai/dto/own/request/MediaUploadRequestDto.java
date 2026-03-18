package ai.dto.own.request;

import ai.enums.MediaUploadTarget;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaUploadRequestDto {
    MultipartFile file;
    UUID parentId;
    UUID ownerId;
    UUID orgId;
    String visibility;
    String username;
    String unit;
    MediaUploadTarget target;
}
