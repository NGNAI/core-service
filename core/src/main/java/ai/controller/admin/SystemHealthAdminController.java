package ai.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.response.SystemHealthResponseDto;
import ai.model.ApiResponseModel;
import ai.security.AdminAccessGuard;
import ai.service.SystemHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Controller admin cung cấp API kiểm tra trạng thái hệ thống cho admin UI.
 * Admin UI (do team khác viết) gọi endpoint này để hiển thị status các external dependencies.
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/system-health")
@Tag(name = "System Health Admin", description = "Admin APIs for checking system health status of external dependencies")
@RestController
public class SystemHealthAdminController {

    SystemHealthService systemHealthService;
    AdminAccessGuard adminAccessGuard;

    @Operation(
        summary = "Kiểm tra quyền truy cập",
        description = "Kiểm tra token hiện tại có quyền truy cập System Health admin APIs (dựa trên danh sách username được phép cấu hình trong hệ thống)"
    )
    @GetMapping("/access")
    public ResponseEntity<ApiResponseModel<Boolean>> checkAccess() {
        return ResponseEntity.ok(
                ApiResponseModel.<Boolean>builder()
                        .message("Check access successfully")
                        .data(adminAccessGuard.isAllowed())
                        .build()
        );
    }

    @Operation(
        summary = "Kiểm tra trạng thái hệ thống",
        description = "Kiểm tra trạng thái tất cả external dependencies (MinIO, RagAPI, IngestionAPI, ...) và trả về kết quả tổng hợp"
    )
    @ApiResponse(responseCode = "200", description = "Kiểm tra thành công",
                 content = @Content(schema = @Schema(implementation = SystemHealthResponseDto.class)))
    @GetMapping
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    public ResponseEntity<ApiResponseModel<SystemHealthResponseDto>> getHealth() {
        SystemHealthResponseDto health = systemHealthService.checkAll();
        return ResponseEntity.ok(
                ApiResponseModel.<SystemHealthResponseDto>builder()
                        .message("Kiểm tra trạng thái hệ thống thành công")
                        .data(health)
                        .build()
        );
    }
}