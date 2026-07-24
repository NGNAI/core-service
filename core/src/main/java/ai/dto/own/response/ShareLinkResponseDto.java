package ai.dto.own.response;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import ai.enums.ShareResource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Response cho share link (owner view).
 */
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonPropertyOrder({
        "id",
        "token",
        "url",
        "resourceType",
        "resourceId",
        "resourceTitle",
        "title",
        "passwordRequired",
        "expiresAt",
        "revokedAt",
        "active",
        "viewCount",
        "lastViewedAt",
        "createdAt",
        "createdBy"
})
public class ShareLinkResponseDto {
    UUID id;

    @Schema(description = "Token ngẫu nhiên dùng trong URL public share")
    String token;

    @Schema(description = "URL đầy đủ để share (build từ share.baseUrl + token). null nếu chưa cấu hình baseUrl.")
    String url;

    ShareResource resourceType;

    UUID resourceId;

    @Schema(description = "Tiêu đề của Topic/Notebook được share (snapshot lúc tạo link)")
    String resourceTitle;

    @Schema(description = "Mô tả link do owner đặt")
    String title;

    @Schema(description = "true = link yêu cầu password khi truy cập")
    boolean passwordRequired;

    @Schema(description = "Thời điểm hết hạn. null = vĩnh viễn.")
    Instant expiresAt;

    @Schema(description = "Thời điểm link bị hủy (revoke). null = còn hiệu lực.")
    Instant revokedAt;

    @Schema(description = "true = link đang active (chưa revoke, chưa hết hạn)")
    boolean active;

    long viewCount;

    Instant lastViewedAt;

    Instant createdAt;

    UUID createdBy;
}