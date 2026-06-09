package ai.controller.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.request.filter.AuditLogFilterDto;
import ai.dto.own.response.AuditLogResponseDto;
import ai.dto.own.response.dashboard.RecentActivitiesDto;
import ai.model.ApiResponseModel;
import ai.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "Audit Log", description = "Audit log management APIs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/audit-logs")
@RestController
public class AuditLogController {

    AuditLogService auditLogService;

    @Operation(
        summary = "Get audit logs list",
        description = "Retrieve audit logs with filtering capabilities. Supports filters: userId, orgId, action, resource, status, from, to, keyword; paginated by pageNumber/pageSize, ordered by createdAt DESC."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved audit logs",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = RecentActivitiesDto.class)))
    @GetMapping
    public ResponseEntity<ApiResponseModel<List<AuditLogResponseDto>>> getAuditLogs(
            @ModelAttribute AuditLogFilterDto filterDto) {
        if (filterDto.getPageSize() == null) {
            filterDto.setPageSize(20);
        }
        
        Page<AuditLogResponseDto> auditLogsPage = auditLogService.getAll(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<AuditLogResponseDto>>builder()
                        .message("Get audit logs successfully")
                        .count(auditLogsPage.getTotalElements())
                        .data(auditLogsPage.getContent())
                        .build());
    }

    @Operation(
        summary = "Get audit log detail",
        description = "Retrieve detailed information of a specific audit log by ID"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved audit log detail",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = AuditLogResponseDto.class)))
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseModel<AuditLogResponseDto>> getAuditLogDetail(
            @PathVariable UUID id) {
        AuditLogResponseDto auditLog = auditLogService.getById(id);
        return ResponseEntity.ok(
                ApiResponseModel.<AuditLogResponseDto>builder()
                        .message("Get audit log detail successfully")
                        .data(auditLog)
                        .build());
    }
}