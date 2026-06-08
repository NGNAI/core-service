package ai.dto.own.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataIngestionUpdateFolderRequestDto {
    @Schema(description = "Tên mới của thư mục, nếu muốn đổi tên", example = "New Folder Name")
    String name;
    
    @Schema(description = "ID của thư mục cha mới, nếu muốn di chuyển thư mục", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID parentId;

    @Schema(description = "Nếu muốn di chuyển thư mục lên root, set true. Lưu ý: parentId sẽ bị bỏ qua nếu moveToRoot là true", example = "true")
    Boolean moveToRoot;

    @NotNull(message = "Organization ID for permission checking cannot be null")
    @Schema(description = "Organization ID for permission checking, should be provided if the folder is not under personal scope", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID organizationId;
}
