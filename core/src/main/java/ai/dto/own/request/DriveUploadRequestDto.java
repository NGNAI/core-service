package ai.dto.own.request;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import ai.constant.InputValidateKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriveUploadRequestDto {
    @Schema(description = "File can upload to personal drive")
    @NotNull(message = InputValidateKey.DRIVE_FILE_REQUIRED)
    MultipartFile file = null;

    @Schema(description = "Folder ID if user uploads into a specific folder")
    UUID folderId = null;
}
