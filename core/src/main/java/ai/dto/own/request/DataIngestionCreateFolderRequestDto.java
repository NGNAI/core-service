package ai.dto.own.request;

import java.util.UUID;

import ai.annotation.EnumValue;
import ai.constant.InputValidateKey;
import ai.enums.DataScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataIngestionCreateFolderRequestDto {
    @Schema(description = "Tên của thư mục cần tạo", example = "New Folder")
    @NotBlank(message = InputValidateKey.DATA_INGESTION_NAME_REQUIRED)
    String name;

    @Schema(description = "ID của thư mục cha, nếu muốn tạo thư mục con. Nếu không cung cấp hoặc để null, thư mục sẽ được tạo ở root", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID parentId;

    @Schema(description = "Mức độ truy cập/phạm vi sử dụng", exampleClasses = DataScope.class) 
    @NotNull(message = InputValidateKey.DATA_INGESTION_ACCESS_LEVEL_INVALID)
    @EnumValue(enumClass = DataScope.class, message = InputValidateKey.DATA_INGESTION_ACCESS_LEVEL_INVALID)
    String accessLevel=null;

    public DataScope getAccessLevel(){
        return DataScope.valueOf(accessLevel);
    }
}
