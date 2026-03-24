package ai.dto.own.request;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import ai.enums.DataScope;
import ai.enums.MediaUploadTarget;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaUploadRequestDto {
    @Schema(description = "File cần upload, hỗ trợ tất cả các định dạng file. Kích thước tối đa phụ thuộc vào cấu hình MinIO của hệ thống")
    @NotNull(message = "MEDIA_FILE_REQUIRED")
    MultipartFile file;
    
    @Schema(description = "ID của thư mục cha, nếu muốn upload vào một thư mục cụ thể nào đó. Nếu không cung cấp, file sẽ được upload vào root")
    UUID folderId;

    @Schema(description = "Mức độ truy cập của media sau khi upload thành công", exampleClasses = DataScope.class) 
    @NotNull(message = "MEDIA_ACCESS_LEVEL_INVALID")
    DataScope accessLevel;

    @Schema(description = "Mục tiêu upload của media. Các giá trị hợp lệ: INGESTION, STORAGE", exampleClasses = MediaUploadTarget.class)
    @NotNull(message = "MEDIA_TARGET_INVALID")
    MediaUploadTarget target;
}
