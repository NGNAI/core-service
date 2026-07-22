package ai.controller.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.request.SystemSettingCreateRequestDto;
import ai.dto.own.request.SystemSettingUpdateRequestDto;
import ai.dto.own.response.SystemSettingGroupResponseDto;
import ai.dto.own.response.SystemSettingResponseDto;
import ai.model.ApiResponseModel;
import ai.security.AdminAccessGuard;
import ai.service.SystemSettingService;
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
@RequestMapping("/admin/system-settings")
@Tag(name = "System Setting Admin", description = "Admin APIs for managing system settings")
@RestController
public class SystemSettingAdminController {

    SystemSettingService systemSettingService;
    AdminAccessGuard adminAccessGuard;

    @Operation(summary = "Check access", description = "Kiểm tra token hiện tại có quyền truy cập System Setting admin APIs (dựa trên danh sách username được phép cấu hình trong hệ thống)")
    @GetMapping("/access")
    ResponseEntity<ApiResponseModel<Boolean>> checkAccess() {
        return ResponseEntity.ok(
                ApiResponseModel.<Boolean>builder()
                        .message("Check access successfully")
                        .data(adminAccessGuard.isAllowed())
                        .build()
        );
    }

    @Operation(summary = "Get all settings grouped", description = "Retrieve all system settings grouped by their group name")
    @GetMapping
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<List<SystemSettingGroupResponseDto>>> getAllGrouped() {
        List<SystemSettingGroupResponseDto> grouped = systemSettingService.getAllGrouped();
        return ResponseEntity.ok(
                ApiResponseModel.<List<SystemSettingGroupResponseDto>>builder()
                        .message("Get all system settings successfully")
                        .data(grouped)
                        .build()
        );
    }

    @Operation(summary = "Get setting by key", description = "Retrieve a single system setting by its key")
    @GetMapping("/{key}")
    @ApiResponse(responseCode = "200", description = "Get setting successfully",
                 content = @Content(schema = @Schema(implementation = SystemSettingResponseDto.class)))
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<SystemSettingResponseDto>> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(
                ApiResponseModel.<SystemSettingResponseDto>builder()
                        .message("Get setting successfully")
                        .data(systemSettingService.getByKey(key))
                        .build()
        );
    }

    @Operation(summary = "Get settings by group", description = "Retrieve all settings in a specific group")
    @GetMapping("/groups/{groupName}")
    @ApiResponse(responseCode = "200", description = "Get settings by group successfully",
                 content = @Content(schema = @Schema(implementation = SystemSettingResponseDto.class)))
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<List<SystemSettingResponseDto>>> getByGroup(@PathVariable String groupName) {
        return ResponseEntity.ok(
                ApiResponseModel.<List<SystemSettingResponseDto>>builder()
                        .message("Get settings by group successfully")
                        .data(systemSettingService.getByGroup(groupName))
                        .build()
        );
    }

    @Operation(summary = "Create setting", description = "Create a new system setting")
    @PostMapping
    @ApiResponse(responseCode = "200", description = "Create setting successfully",
                 content = @Content(schema = @Schema(implementation = SystemSettingResponseDto.class)))
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<SystemSettingResponseDto>> create(
            @Valid @RequestBody SystemSettingCreateRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<SystemSettingResponseDto>builder()
                        .message("Create setting successfully")
                        .data(systemSettingService.create(requestDto))
                        .build()
        );
    }

    @Operation(summary = "Update setting", description = "Update an existing system setting by its key")
    @PutMapping("/{key}")
    @ApiResponse(responseCode = "200", description = "Update setting successfully",
                 content = @Content(schema = @Schema(implementation = SystemSettingResponseDto.class)))
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<SystemSettingResponseDto>> update(
            @PathVariable String key,
            @Valid @RequestBody SystemSettingUpdateRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<SystemSettingResponseDto>builder()
                        .message("Update setting successfully")
                        .data(systemSettingService.update(key, requestDto))
                        .build()
        );
    }

    @Operation(summary = "Bulk update settings", description = "Update multiple system settings at once")
    @PutMapping("/bulk")
    @ApiResponse(responseCode = "200", description = "Bulk update settings successfully",
                 content = @Content(schema = @Schema(implementation = SystemSettingResponseDto.class)))
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<List<SystemSettingResponseDto>>> bulkUpdate(
            @Valid @RequestBody List<SystemSettingUpdateRequestDto> requestDtos) {
        return ResponseEntity.ok(
                ApiResponseModel.<List<SystemSettingResponseDto>>builder()
                        .message("Bulk update settings successfully")
                        .data(systemSettingService.bulkUpdate(requestDtos))
                        .build()
        );
    }

    @Operation(summary = "Delete setting by key", description = "Delete a system setting by its key")
    @DeleteMapping("/{key}")
    @ApiResponse(responseCode = "200", description = "Delete setting successfully",
                 content = @Content(schema = @Schema(implementation = Void.class)))
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable String key) {
        systemSettingService.delete(key);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete setting successfully")
                        .build()
        );
    }

    @Operation(summary = "Delete setting by ID", description = "Delete a system setting by its UUID")
    @DeleteMapping("/id/{id}")
    @ApiResponse(responseCode = "200", description = "Delete setting successfully",
                 content = @Content(schema = @Schema(implementation = Void.class)))
    @PreAuthorize("@adminAccessGuard.isAllowed()")
    ResponseEntity<ApiResponseModel<Void>> deleteById(@PathVariable UUID id) {
        systemSettingService.deleteById(id);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete setting successfully")
                        .build()
        );
    }
}
