package ai.dto.own.request;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import ai.annotation.EnumValue;
import ai.constant.InputValidateKey;
import ai.enums.DataScope;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataIngestionUploadRequestDto {
    @Schema(description = "File cần upload, hỗ trợ tất cả các định dạng file. Kích thước tối đa phụ thuộc vào cấu hình MinIO của hệ thống")
    @NotNull(message = InputValidateKey.DATA_INGESTION_FILE_REQUIRED)
    MultipartFile file=null;
    
    @Schema(description = "ID của thư mục cha, nếu muốn upload vào một thư mục cụ thể nào đó. Nếu không cung cấp, file sẽ được upload vào root")
    UUID folderId=null;

    @NotNull(message = InputValidateKey.DATA_INGESTION_ACCESS_LEVEL_INVALID)
    @Schema(description = "Organization ID for permission checking, should be provided if the file is not uploaded under personal scope", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID organizationId=null;

    @Schema(description = "Mức độ truy cập/phạm vi sử dụng", exampleClasses = DataScope.class) 
    @NotNull(message = InputValidateKey.DATA_INGESTION_ACCESS_LEVEL_INVALID)
    @EnumValue(enumClass = DataScope.class, message = InputValidateKey.DATA_INGESTION_ACCESS_LEVEL_INVALID)
    String accessLevel=null;

    @Hidden
    @Schema(description = "URL callback để ingestion service gọi về khi có cập nhật trạng thái job", example = "https://api.example.com/api/v1/user/data-ingestion/ingestion/webhook/status")
    String callbackUrl=null;

    public DataScope getAccessLevel(){
        return DataScope.valueOf(accessLevel);
    }
}
