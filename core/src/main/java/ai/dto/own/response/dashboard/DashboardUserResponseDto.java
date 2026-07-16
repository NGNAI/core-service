package ai.dto.own.response.dashboard;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Dashboard summary for current user")
public class DashboardUserResponseDto {

    @Schema(description = "Tổng số chủ đề (Topics) của người dùng")
    long totalTopics;

    @Schema(description = "Tổng số sổ tay (Notebooks) của người dùng")
    long totalNoteBooks;

    @Schema(description = "Tổng số ghi chú (Notes) của người dùng")
    long totalNotes;

    @Schema(description = "Phân loại ghi chú theo loại (source type)")
    Map<String, Long> notesBySourceType;

    @Schema(description = "Tổng số dữ liệu đã nhập (Data Ingestions) của người dùng")
    long totalDataIngestions;

    @Schema(description = "Phân loại dữ liệu nhập theo trạng thái (IngestionStatus)")
    Map<String, Long> dataIngestionsByStatus;

    @Schema(description = "Tổng số thao tác (actions) trong khoảng thời gian")
    long totalActions;

    @Schema(description = "Tổng số lần đăng nhập trong khoảng thời gian")
    long totalLogins;

    @Schema(description = "Xu hướng hoạt động theo ngày")
    List<DailyActivityDto> dailyActivityTrend;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Hoạt động theo ngày")
    public static class DailyActivityDto {

        @Schema(description = "Ngày (ISO-8601)")
        String date;

        @Schema(description = "Số thao tác trong ngày")
        long actionCount;

        @Schema(description = "Số lần đăng nhập trong ngày")
        long loginCount;
    }
}