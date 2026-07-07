package ai.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.response.SystemSettingResponseDto;
import ai.model.ApiResponseModel;
import ai.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/public/settings")
@Tag(name = "System Setting Public", description = "Public system settings APIs (no authentication required)")
@RestController
public class SystemSettingPublicController {

    SystemSettingService systemSettingService;

    @Operation(summary = "Get public settings", description = "Retrieve all public system settings (e.g., system name, contact info, social links)")
    @GetMapping
    ResponseEntity<ApiResponseModel<List<SystemSettingResponseDto>>> getPublicSettings() {
        List<SystemSettingResponseDto> settings = systemSettingService.getPublicSettings();
        return ResponseEntity.ok(
                ApiResponseModel.<List<SystemSettingResponseDto>>builder()
                        .message("Get public settings successfully")
                        .data(settings)
                        .build()
        );
    }

    @Operation(summary = "Get public settings as map", description = "Retrieve all public system settings as a key-value map for easy consumption")
    @GetMapping("/map")
    ResponseEntity<ApiResponseModel<Map<String, String>>> getPublicSettingsMap() {
        Map<String, String> settingsMap = systemSettingService.getPublicSettingsMap();
        return ResponseEntity.ok(
                ApiResponseModel.<Map<String, String>>builder()
                        .message("Get public settings map successfully")
                        .data(settingsMap)
                        .build()
        );
    }

    @Operation(summary = "Get public settings by group", description = "Retrieve public settings filtered by group name")
    @GetMapping("/groups/{groupName}")
    ResponseEntity<ApiResponseModel<List<SystemSettingResponseDto>>> getPublicSettingsByGroup(
            @PathVariable String groupName) {
        List<SystemSettingResponseDto> settings = systemSettingService.getPublicSettings().stream()
                .filter(s -> s.getGroupName().equalsIgnoreCase(groupName))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(
                ApiResponseModel.<List<SystemSettingResponseDto>>builder()
                        .message("Get public settings by group successfully")
                        .data(settings)
                        .build()
        );
    }
}
