package ai.dto.own.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * DTO phản hồi trạng thái hệ thống tổng hợp cho admin UI.
 * Chứa trạng thái tổng và danh sách chi tiết từng thành phần.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemHealthResponseDto {

    /**
     * Trạng thái tổng hợp: "UP" (tất cả OK) hoặc "DOWN" (có thành phần lỗi).
     */
    String status;

    /**
     * Thời điểm kiểm tra.
     */
    Instant checkedAt;

    /**
     * Thời gian kiểm tra tổng cộng (ms).
     */
    long totalCheckTimeMs;

    /**
     * Danh sách chi tiết trạng thái từng thành phần.
     */
    List<ComponentHealthDto> components;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ComponentHealthDto {
        /** Tên thành phần (minio, ragApi, ingestionApi, database, redis...) */
        String name;
        /** Trạng thái: "UP" hoặc "DOWN" */
        String status;
        /** Chi tiết bổ sung (endpoint, version, response...) */
        String detail;
        /** Thông báo lỗi nếu DOWN */
        String error;
        /** Thời gian kiểm tra thành phần này (ms) */
        long checkTimeMs;
    }
}