package ai.controller.admin.report;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.request.report.ActivityReportFilterDto;
import ai.dto.own.response.report.ActivityReportResponseDto;
import ai.dto.own.response.report.ActivityReportResponseDto.DailyActivityDto;
import ai.dto.own.response.report.ActivityReportResponseDto.LoginFrequencySummary;
import ai.dto.own.response.report.ActivityReportResponseDto.UserActivitySummary;
import ai.model.ApiResponseModel;
import ai.service.report.ActivityReportService;
import ai.util.CsvUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/reports/activities")
@Tag(name = "Activity Report", description = "Activity report APIs")
@RestController
public class ActivityReportAdminController {

    ActivityReportService activityReportService;

    @Operation(
        summary = "Get activity report summary",
        description = "Retrieve activity statistics for an organization (and its descendants if includeDescendants=true). " +
                      "Returns total actions, logins, unique active users, breakdowns by resource/action, top active users, login frequency, and daily trend."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved activity report",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ActivityReportResponseDto.class)))
    @GetMapping("/summary")
    @PreAuthorize("@perm.canAccess(#filter.orgId, 'REPORT', 'READ', null)")
    public ResponseEntity<ApiResponseModel<ActivityReportResponseDto>> getActivitySummary(
            @Valid @ModelAttribute ActivityReportFilterDto filter) {
        ActivityReportResponseDto report = activityReportService.getActivityReport(filter);
        return ResponseEntity.ok(
                ApiResponseModel.<ActivityReportResponseDto>builder()
                        .message("Lấy báo cáo hoạt động thành công")
                        .data(report)
                        .build());
    }

    @Operation(
        summary = "Get top active users",
        description = "Retrieve the most active users in the system for a given period and organization."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved top active users",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = UserActivitySummary.class)))
    @GetMapping("/top-users")
    @PreAuthorize("@perm.canAccess(#filter.orgId, 'REPORT', 'READ', null)")
    public ResponseEntity<ApiResponseModel<List<UserActivitySummary>>> getTopActiveUsers(
            @Valid @ModelAttribute ActivityReportFilterDto filter) {
        List<UserActivitySummary> topUsers = activityReportService.getTopActiveUsers(filter);
        return ResponseEntity.ok(
                ApiResponseModel.<List<UserActivitySummary>>builder()
                        .message("Lấy danh sách người dùng tích cực thành công")
                        .data(topUsers)
                        .build());
    }

    @Operation(
        summary = "Get login frequency",
        description = "Retrieve login frequency statistics for users in the system."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved login frequency",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = LoginFrequencySummary.class)))
    @GetMapping("/login-frequency")
    @PreAuthorize("@perm.canAccess(#filter.orgId, 'REPORT', 'READ', null)")
    public ResponseEntity<ApiResponseModel<List<LoginFrequencySummary>>> getLoginFrequency(
            @Valid @ModelAttribute ActivityReportFilterDto filter) {
        List<LoginFrequencySummary> loginFreq = activityReportService.getLoginFrequency(filter);
        return ResponseEntity.ok(
                ApiResponseModel.<List<LoginFrequencySummary>>builder()
                        .message("Lấy tần suất đăng nhập thành công")
                        .data(loginFreq)
                        .build());
    }

    @Operation(
        summary = "Export activity report as CSV",
        description = "Export activity statistics to CSV file."
    )
    @ApiResponse(responseCode = "200", description = "CSV file downloaded successfully",
        content = @Content(mediaType = "text/csv"))
    @GetMapping("/export")
    @PreAuthorize("@perm.canAccess(#filter.orgId, 'REPORT', 'READ', null)")
    public ResponseEntity<InputStreamResource> exportActivityReport(
            @Valid @ModelAttribute ActivityReportFilterDto filter) {
        ActivityReportResponseDto report = activityReportService.getActivityReport(filter);

        List<String> headers = List.of("Ngày", "Số thao tác", "Số đăng nhập");
        List<List<String>> rows = new ArrayList<>();

        // Summary section
        rows.add(List.of("TỔNG QUAN", "", ""));
        rows.add(List.of("Tổng số thao tác", String.valueOf(report.getTotalActions()), ""));
        rows.add(List.of("Tổng số đăng nhập", String.valueOf(report.getTotalLogins()), ""));
        rows.add(List.of("Người dùng hoạt động", String.valueOf(report.getUniqueActiveUsers()), ""));
        rows.add(List.of("", "", ""));

        // Top users section
        rows.add(List.of("TOP NGƯỜI DÙNG TÍCH CỰC", "", ""));
        rows.add(List.of("STT", "Tên người dùng", "Số thao tác"));
        int stt = 1;
        for (UserActivitySummary user : report.getTopActiveUsers()) {
            rows.add(List.of(
                    String.valueOf(stt++),
                    user.getUserName(),
                    String.valueOf(user.getActionCount())
            ));
        }
        rows.add(List.of("", "", ""));

        // Daily trend section
        rows.add(List.of("XU HƯỚNG THEO NGÀY", "", ""));
        rows.add(List.of("Ngày", "Số thao tác", "Số đăng nhập"));
        for (DailyActivityDto daily : report.getDailyTrend()) {
            rows.add(List.of(
                    daily.getDate(),
                    String.valueOf(daily.getActionCount()),
                    String.valueOf(daily.getLoginCount())
            ));
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        CsvUtil.writeCsv(baos, headers, rows);
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bao-cao-hoat-dong.csv");
        httpHeaders.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));

        return ResponseEntity.ok()
                .headers(httpHeaders)
                .body(new InputStreamResource(bais));
    }
}