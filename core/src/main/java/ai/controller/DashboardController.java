package ai.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.response.dashboard.DashboardOverviewDto;
import ai.dto.own.response.dashboard.DataIngestionStatisticsDto;
import ai.dto.own.response.dashboard.DraftStatisticsDto;
import ai.dto.own.response.dashboard.TimelineStatisticsDto;
import ai.model.ApiResponseModel;
import ai.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "Dashboard", description = "Dashboard statistics APIs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/dashboard")
@RestController
public class DashboardController {

    DashboardService dashboardService;

    @Operation(
        summary = "Get dashboard overview",
        description = "Retrieve overall statistics for the dashboard including totals for drafts, topics, notebooks, data ingestions, users, and organizations"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved dashboard overview", 
        content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = DashboardOverviewDto.class)))
    @GetMapping("/overview")
    public ResponseEntity<ApiResponseModel<DashboardOverviewDto>> getOverview() {
        DashboardOverviewDto overview = dashboardService.getOverview();
        return ResponseEntity.ok(
                ApiResponseModel.<DashboardOverviewDto>builder()
                        .message("Get dashboard overview successfully")
                        .data(overview)
                        .build());
    }

    @Operation(
        summary = "Get draft statistics",
        description = "Retrieve statistics for drafts including total count and breakdown by type and presentation style"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved draft statistics", 
        content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = DraftStatisticsDto.class)))
    @GetMapping("/drafts/statistics")
    public ResponseEntity<ApiResponseModel<DraftStatisticsDto>> getDraftStatistics() {
        DraftStatisticsDto statistics = dashboardService.getDraftStatistics();
        return ResponseEntity.ok(
                ApiResponseModel.<DraftStatisticsDto>builder()
                        .message("Get draft statistics successfully")
                        .data(statistics)
                        .build());
    }

    @Operation(
        summary = "Get data ingestion statistics",
        description = "Retrieve statistics for data ingestions including total count and breakdown by status and source"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved data ingestion statistics", 
        content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = DataIngestionStatisticsDto.class)))
    @GetMapping("/data-ingestion/statistics")
    public ResponseEntity<ApiResponseModel<DataIngestionStatisticsDto>> getDataIngestionStatistics() {
        DataIngestionStatisticsDto statistics = dashboardService.getDataIngestionStatistics();
        return ResponseEntity.ok(
                ApiResponseModel.<DataIngestionStatisticsDto>builder()
                        .message("Get data ingestion statistics successfully")
                        .data(statistics)
                        .build());
    }

    @Operation(
        summary = "Get timeline statistics",
        description = "Retrieve daily statistics for a date range including counts of drafts, topics, notebooks, and data ingestions"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved timeline statistics", 
        content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = TimelineStatisticsDto.class)))
    @GetMapping("/timeline")
    public ResponseEntity<ApiResponseModel<TimelineStatisticsDto>> getTimelineStatistics(
            @Parameter(description = "Start date for timeline statistics (format: yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date for timeline statistics (format: yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        TimelineStatisticsDto statistics = dashboardService.getTimelineStatistics(from, to);
        return ResponseEntity.ok(
                ApiResponseModel.<TimelineStatisticsDto>builder()
                        .message("Get timeline statistics successfully")
                        .data(statistics)
                        .build());
    }
}