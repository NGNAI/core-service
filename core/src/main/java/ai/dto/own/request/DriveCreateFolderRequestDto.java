package ai.dto.own.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriveCreateFolderRequestDto {
    @Schema(description = "Folder name", example = "Notebook Folder")
    @NotBlank(message = "DRIVE_NAME_REQUIRED")
    String name;

    @Schema(description = "Parent folder ID. If null, folder will be created at root")
    UUID parentId;
}
