package ai.dto.own.request;

import java.util.UUID;

import ai.enums.ShareResource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Request tạo share link cho Topic / Notebook.
 * <p>
 * Quyền mặc định của viewer là read-only (xem metadata, messages, sources, presigned download URL).
 */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShareLinkCreateRequestDto {
    @Schema(description = "Loại tài nguyên được share", example = "TOPIC", required = true)
    @NotNull(message = "Resource type cannot be null")
    ShareResource resourceType;

    @Schema(description = "UUID của Topic / Notebook cần share", required = true)
    @NotNull(message = "Resource id cannot be null")
    UUID resourceId;

    @Schema(description = "Mô tả link (vd 'Gửi cho team A'). Bỏ trống nếu không cần.")
    String title;

    @Schema(description = "Password bảo vệ link (tùy chọn). null = ai có link vào được.")
    String password;

    @Schema(description = "Số ngày hết hạn. null = vĩnh viễn. Tối đa theo cấu hình share.maxExpiryDays (mặc định 365).", example = "30")
    @Min(value = 1, message = "Expiry days must be at least 1")
    Integer expiryDays;
}