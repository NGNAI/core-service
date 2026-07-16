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

import ai.dto.own.request.report.DataReportFilterDto;
import ai.dto.own.response.report.ContentStatsDto;
import ai.dto.own.response.report.DataIngestionDetailDto;
import ai.dto.own.response.report.DataIngestionDetailDto.OwnerIngestionSummary;
import ai.dto.own.response.report.DataReportResponseDto;
import ai.model.ApiResponseModel;
import ai.service.report.DataReportService;
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
@RequestMapping("/admin/reports/data")
@Tag(name = "Data Report", description = "Data report APIs")
@RestController
public class DataReportAdminController {

    DataReportService dataReportService;

    @Operation(
        summary = "Get data report summary",
        description = "Retrieve overall data statistics including ingestions, drafts, topics, notebooks, and notes."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved data report",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = DataReportResponseDto.class)))
    @GetMapping("/summary")
    @PreAuthorize("@perm.canAccess(null, 'REPORT', 'READ', null)")
    public ResponseEntity<ApiResponseModel<DataReportResponseDto>> getDataSummary(
            @Valid @ModelAttribute DataReportFilterDto filter) {
        DataReportResponseDto report = dataReportService.getDataReport(filter);
        return ResponseEntity.ok(
                ApiResponseModel.<DataReportResponseDto>builder()
                        .message("Lấy báo cáo dữ liệu thành công")
                        .data(report)
                        .build());
    }

    @Operation(
        summary = "Get data ingestion detail",
        description = "Retrieve detailed ingestion statistics: by status, source, access level, top owners, file size, content type."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved ingestion detail",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = DataIngestionDetailDto.class)))
    @GetMapping("/ingestion/detail")
    @PreAuthorize("@perm.canAccess(null, 'REPORT', 'READ', null)")
    public ResponseEntity<ApiResponseModel<DataIngestionDetailDto>> getIngestionDetail(
            @Valid @ModelAttribute DataReportFilterDto filter) {
        DataIngestionDetailDto detail = dataReportService.getIngestionDetail(filter);
        return ResponseEntity.ok(
                ApiResponseModel.<DataIngestionDetailDto>builder()
                        .message("Lấy chi tiết dữ liệu nhập thành công")
                        .data(detail)
                        .build());
    }

    @Operation(
        summary = "Get top data ingestion owners",
        description = "Retrieve the top N users who have ingested the most data."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved top owners",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = OwnerIngestionSummary.class)))
    @GetMapping("/ingestion/by-owner")
    @PreAuthorize("@perm.canAccess(null, 'REPORT', 'READ', null)")
    public ResponseEntity<ApiResponseModel<List<OwnerIngestionSummary>>> getTopOwners(
            @Valid @ModelAttribute DataReportFilterDto filter) {
        List<OwnerIngestionSummary> topOwners = dataReportService.getTopOwners(filter);
        return ResponseEntity.ok(
                ApiResponseModel.<List<OwnerIngestionSummary>>builder()
                        .message("Lấy danh sách người dùng nhập nhiều dữ liệu thành công")
                        .data(topOwners)
                        .build());
    }

    @Operation(
        summary = "Get content statistics",
        description = "Retrieve content statistics: drafts by type, topics, notebooks, notes by source type."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved content statistics",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ContentStatsDto.class)))
    @GetMapping("/content-stats")
    @PreAuthorize("@perm.canAccess(null, 'REPORT', 'READ', null)")
    public ResponseEntity<ApiResponseModel<ContentStatsDto>> getContentStats(
            @Valid @ModelAttribute DataReportFilterDto filter) {
        ContentStatsDto stats = dataReportService.getContentStats(filter);
        return ResponseEntity.ok(
                ApiResponseModel.<ContentStatsDto>builder()
                        .message("Lấy thống kê nội dung thành công")
                        .data(stats)
                        .build());
    }

    @Operation(
        summary = "Export data report as CSV",
        description = "Export data statistics to CSV file."
    )
    @ApiResponse(responseCode = "200", description = "CSV file downloaded successfully",
        content = @Content(mediaType = "text/csv"))
    @GetMapping("/export")
    @PreAuthorize("@perm.canAccess(null, 'REPORT', 'READ', null)")
    public ResponseEntity<InputStreamResource> exportDataReport(
            @Valid @ModelAttribute DataReportFilterDto filter) {
        DataReportResponseDto report = dataReportService.getDataReport(filter);

        List<String> headers = List.of("Loại", "Số lượng");
        List<List<String>> rows = new ArrayList<>();

        rows.add(List.of("TỔNG QUAN DỮ LIỆU", ""));
        rows.add(List.of("Dữ liệu đã nhập (Ingestions)", String.valueOf(report.getTotalDataIngestions())));
        rows.add(List.of("Bản nháp (Drafts)", String.valueOf(report.getTotalDrafts())));
        rows.add(List.of("Chủ đề (Topics)", String.valueOf(report.getTotalTopics())));
        rows.add(List.of("Sổ tay (Notebooks)", String.valueOf(report.getTotalNoteBooks())));
        rows.add(List.of("Ghi chú (Notes)", String.valueOf(report.getTotalNotes())));

        if (report.getIngestionDetail() != null) {
            rows.add(List.of("", ""));
            rows.add(List.of("CHI TIẾT INGESTION", ""));
            rows.add(List.of("Tổng dung lượng (bytes)", String.valueOf(report.getIngestionDetail().getTotalFileSize())));

            if (report.getIngestionDetail().getByStatus() != null) {
                rows.add(List.of("", ""));
                rows.add(List.of("THEO TRẠNG THÁI", ""));
                report.getIngestionDetail().getByStatus().forEach((k, v) ->
                    rows.add(List.of(k, String.valueOf(v))));
            }
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        CsvUtil.writeCsv(baos, headers, rows);
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bao-cao-du-lieu.csv");
        httpHeaders.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));

        return ResponseEntity.ok()
                .headers(httpHeaders)
                .body(new InputStreamResource(bais));
    }
}