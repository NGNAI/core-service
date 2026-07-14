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

import ai.dto.own.request.report.UserReportFilterDto;
import ai.dto.own.response.report.UserReportResponseDto;
import ai.dto.own.response.report.UserReportResponseDto.OrgUserSummaryDto;
import ai.dto.own.response.report.UserReportResponseDto.RoleUserSummaryDto;
import ai.model.ApiResponseModel;
import ai.service.report.UserReportService;
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
@RequestMapping("/admin/reports/users")
@Tag(name = "User Report", description = "User report APIs")
@RestController
public class UserReportControllerAdmin {

    UserReportService userReportService;

    @Operation(
        summary = "Get user report summary",
        description = "Retrieve user statistics for an organization (and its descendants if includeDescendants=true). " +
                      "Returns total users, active/inactive counts, breakdown by organization, and breakdown by role."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user report",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = UserReportResponseDto.class)))
    @GetMapping("/summary")
    @PreAuthorize("@perm.canAccess(#filter.orgId, 'REPORT', 'READ', null)")
    public ResponseEntity<ApiResponseModel<UserReportResponseDto>> getUserSummary(
            @Valid @ModelAttribute UserReportFilterDto filter) {
        UserReportResponseDto report = userReportService.getUserReport(filter);
        return ResponseEntity.ok(
                ApiResponseModel.<UserReportResponseDto>builder()
                        .message("Lấy báo cáo người dùng thành công")
                        .data(report)
                        .build());
    }

    @Operation(
        summary = "Export user report as CSV",
        description = "Export user statistics to CSV file. Supports the same filters as the summary endpoint."
    )
    @ApiResponse(responseCode = "200", description = "CSV file downloaded successfully",
        content = @Content(mediaType = "text/csv"))
    @GetMapping("/export")
    @PreAuthorize("@perm.canAccess(#filter.orgId, 'REPORT', 'READ', null)")
    public ResponseEntity<InputStreamResource> exportUserReport(
            @Valid @ModelAttribute UserReportFilterDto filter) {
        UserReportResponseDto report = userReportService.getUserReport(filter);

        // Build CSV content
        List<String> headers = List.of("STT", "Tên đơn vị", "Mã đơn vị", "Tổng người dùng", "Đang hoạt động", "Không hoạt động");
        List<List<String>> rows = new ArrayList<>();
        int stt = 1;
        for (OrgUserSummaryDto org : report.getOrgSummaries()) {
            rows.add(List.of(
                    String.valueOf(stt++),
                    org.getOrgName(),
                    org.getOrgId() != null ? org.getOrgId().toString() : "",
                    String.valueOf(org.getTotalUsers()),
                    String.valueOf(org.getActiveUsers()),
                    String.valueOf(org.getInactiveUsers())
            ));
        }

        // Add summary row for role breakdown
        rows.add(List.of("", "--- PHÂN BỔ THEO VAI TRÒ ---", "", "", "", ""));
        rows.add(List.of("STT", "Tên vai trò", "Mã vai trò", "Số người dùng", "", ""));
        stt = 1;
        for (RoleUserSummaryDto role : report.getRoleSummaries()) {
            rows.add(List.of(
                    String.valueOf(stt++),
                    role.getRoleName(),
                    role.getRoleId() != null ? role.getRoleId().toString() : "",
                    String.valueOf(role.getTotalUsers()),
                    "",
                    ""
            ));
        }

        // Write to CSV
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        CsvUtil.writeCsv(baos, headers, rows);
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bao-cao-nguoi-dung.csv");
        httpHeaders.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));

        return ResponseEntity.ok()
                .headers(httpHeaders)
                .body(new InputStreamResource(bais));
    }
}