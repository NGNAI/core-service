package ai.dto.own.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Thống kê lượt xem trên một share link.
 */
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShareLinkStatsDto {
    @Schema(description = "Tổng số lượt xem")
    long viewCount;

    @Schema(description = "Thời điểm xem gần nhất")
    Instant lastViewedAt;

    @Schema(description = "Trạng thái active (chưa revoke + chưa hết hạn)")
    boolean active;
}