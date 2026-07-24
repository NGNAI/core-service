package ai.dto.own.request.filter;

import ai.enums.ShareResource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Filter cho list share link của owner.
 */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShareLinkFilterDto extends PageableFilterDto {
    @Schema(description = "Lọc theo loại tài nguyên (TOPIC / NOTEBOOK). Bỏ trống = tất cả.")
    ShareResource resourceType;

    @Schema(description = "Lọc theo UUID tài nguyên cụ thể. Bỏ trống = tất cả.")
    java.util.UUID resourceId;

    @Schema(description = "true = chỉ trả link còn hiệu lực (chưa revoke, chưa hết hạn). Bỏ trống = tất cả.")
    Boolean activeOnly;

    public ShareLinkFilterDto() {
        // sort mặc định theo createdAt DESC
        setSortBy("createdAt");
        setSortDir("DESC");
    }
}