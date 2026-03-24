package ai.dto.own.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaUpdateFolderRequestDto {
    @Schema(description = "Tên mới của thư mục, nếu muốn đổi tên")
    String name;
    
    @Schema(description = "ID của thư mục cha mới, nếu muốn di chuyển thư mục")
    UUID parentId;

    @Schema(description = "Nếu muốn di chuyển thư mục lên root, set true. Lưu ý: parentId sẽ bị bỏ qua nếu moveToRoot là true")
    Boolean moveToRoot;
}
