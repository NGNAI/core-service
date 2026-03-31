package ai.dto.own.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataIngestionCreateFolderRequestDto {
    @Schema(description = "Tên của thư mục cần tạo", example = "New Folder")
    @NotBlank(message = "DATA_INGESTION_NAME_REQUIRED")
    String name;

    @Schema(description = "ID của thư mục cha, nếu muốn tạo thư mục con. Nếu không cung cấp hoặc để null, thư mục sẽ được tạo ở root", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID parentId;
}
