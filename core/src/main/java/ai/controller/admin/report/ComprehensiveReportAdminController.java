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

import ai.dto.own.request.report.ComprehensiveReportFilterDto;
import ai.dto.own.response.report.ActivityReportResponseDto.DailyActivityDto;
import ai.dto.own.response.report.ActivityReportResponseDto.UserActivitySummary;
import ai.dto.own.response.report.ComprehensiveReportResponseDto;
import ai.model.ApiResponseModel;
import ai.service.report.ComprehensiveReportService;
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
@RequestMapping("/admin/reports/comprehensive")
@Tag(name = "Comprehensive Report", description = "Comprehensive report APIs - all metrics in one response")
@RestController
public class ComprehensiveReportAdminController {

    ComprehensiveReportService comprehensiveReportService;

    @Operation(
        summary = "Get comprehensive report",
        description = "Retrieve all metrics in one response: user stats, content stats, activity stats, top users, and daily trend."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved comprehensive report",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ComprehensiveReportResponseDto.class)))
    @GetMapping
    @PreAuthorize("@perm.canAccess(#filter.orgId, 'REPORT', 'READ', null)")
    public ResponseEntity<ApiResponseModel<ComprehensiveReportResponseDto>> getComprehensiveReport(
            @Valid @ModelAttribute ComprehensiveReportFilterDto filter) {
        ComprehensiveReportResponseDto report = comprehensiveReportService.getComprehensiveReport(filter);
        return ResponseEntity.ok(
                ApiResponseModel.<ComprehensiveReportResponseDto>builder()
                        .message("Lấy báo cáo tổng hợp thành công")
                        .data(report)
                        .build());
    }

    @Operation(
        summary = "Export comprehensive report as CSV",
        description = "Export all metrics to CSV file."
    )
    @ApiResponse(responseCode = "200", description = "CSV file downloaded successfully",
        content = @Content(mediaType = "text/csv"))
    @GetMapping("/export")
    @PreAuthorize("@perm.canAccess(#filter.orgId, 'REPORT', 'READ', null)")
    public ResponseEntity<InputStreamResource> exportComprehensiveReport(
            @Valid @ModelAttribute ComprehensiveReportFilterDto filter) {
        ComprehensiveReportResponseDto report = comprehensiveReportService.getComprehensiveReport(filter);

        List<String> headers = List.of("Chỉ số", "Giá trị");
        List<List<String>> rows = new ArrayList<>();

        rows.add(List.of("NGƯỜI DÙNG", ""));
        rows.add(List.of("Tổng người dùng", String.valueOf(report.getTotalUsers())));
        rows.add(List.of("Người dùng đang hoạt động", String.valueOf(report.getActiveUsers())));
        rows.add(List.of("Người dùng không hoạt động", String.valueOf(report.getInactiveUsers())));
        rows.add(List.of("Tổng đơn vị", String.valueOf(report.getTotalOrganizations())));
        rows.add(List.of("", ""));

        rows.add(List.of("NỘI DUNG", ""));
        rows.add(List.of("Bản nháp (Drafts)", String.valueOf(report.getTotalDrafts())));
        rows.add(List.of("Chủ đề (Topics)", String.valueOf(report.getTotalTopics())));
        rows.add(List.of("Sổ tay (Notebooks)", String.valueOf(report.getTotalNoteBooks())));
        rows.add(List.of("Dữ liệu đã nhập", String.valueOf(report.getTotalDataIngestions())));
        rows.add(List.of("Ghi chú (Notes)", String.valueOf(report.getTotalNotes())));
        rows.add(List.of("", ""));

        rows.add(List.of("HOẠT ĐỘNG", ""));
        rows.add(List.of("Tổng thao tác", String.valueOf(report.getTotalActions())));
        rows.add(List.of("Tổng đăng nhập", String.valueOf(report.getTotalLogins())));
        rows.add(List.of("Người dùng hoạt động", String.valueOf(report.getUniqueActiveUsers())));

        if (report.getTopActiveUsers() != null && !report.getTopActiveUsers().isEmpty()) {
            rows.add(List.of("", ""));
            rows.add(List.of("TOP NGƯỜI DÙNG TÍCH CỰC", "Số thao tác"));
            for (UserActivitySummary user : report.getTopActiveUsers()) {
                rows.add(List.of(user.getUserName(), String.valueOf(user.getActionCount())));
            }
        }

        if (report.getRecentDailyTrend() != null && !report.getRecentDailyTrend().isEmpty()) {
            rows.add(List.of("", ""));
            rows.add(List.of("XU HƯỚNG 7 NGÀY GẦN NHẤT", "Thao tác | Đăng nhập"));
            for (DailyActivityDto daily : report.getRecentDailyTrend()) {
                rows.add(List.of(daily.getDate(), daily.getActionCount() + " | " + daily.getLoginCount()));
            }
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        CsvUtil.writeCsv(baos, headers, rows);
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bao-cao-tong-hop.csv");
        httpHeaders.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));

        return ResponseEntity.ok()
                .headers(httpHeaders)
                .body(new InputStreamResource(bais));
    }
}