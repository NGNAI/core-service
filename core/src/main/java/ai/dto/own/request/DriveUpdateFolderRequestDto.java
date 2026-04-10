package ai.dto.own.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriveUpdateFolderRequestDto {
    @Schema(description = "New folder name", example = "Notebook Folder Renamed")
    String name;

    @Schema(description = "New parent folder ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID parentId;

    @Schema(description = "Move folder to root when true", example = "true")
    Boolean moveToRoot;
}
