package ai.dto.own.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataIngestionUpdateFolderRequestDto {
    @Schema(description = "Tên mới của thư mục, nếu muốn đổi tên", example = "New Folder Name")
    String name;
    
    @Schema(description = "ID của thư mục cha mới, nếu muốn di chuyển thư mục", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID parentId;

    @Schema(description = "Nếu muốn di chuyển thư mục lên root, set true. Lưu ý: parentId sẽ bị bỏ qua nếu moveToRoot là true", example = "true")
    Boolean moveToRoot;
}
