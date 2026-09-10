package ai.controller.user;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.response.UploadConfigResponseDto;
import ai.enums.UploadType;
import ai.model.ApiResponseModel;
import ai.service.UploadConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * API cấu hình giới hạn upload cho từng loại (Topic/Notebook/Draft/Data-Ingestion).
 * Nằm trong {@code /user/**} nên yêu cầu xác thực JWT — FE dùng để chặn tại giao diện
 * trước khi gửi file lên server.
 */
@Tag(name = "Upload Config", description = "Upload configuration APIs for frontend validation")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/upload-config")
@RestController
public class UploadConfigUserController {

    UploadConfigService uploadConfigService;

    @Operation(summary = "Get upload config for a specific type", description = "Get upload limits (max file count, max file size, allowed file types, max total sources) for a specific upload type")
    @GetMapping("/{type}")
    ResponseEntity<ApiResponseModel<UploadConfigResponseDto>> getConfig(@PathVariable UploadType type) {
        return ResponseEntity.ok(
                ApiResponseModel.<UploadConfigResponseDto>builder()
                        .message("Get upload config successfully")
                        .data(uploadConfigService.getConfig(type))
                        .build());
    }

    @Operation(summary = "Get upload config for all types", description = "Get upload limits for all upload types (TOPIC, NOTEBOOK, DRAFT, DATA_INGESTION)")
    @GetMapping
    ResponseEntity<ApiResponseModel<List<UploadConfigResponseDto>>> getAllConfigs() {
        List<UploadConfigResponseDto> configs = Arrays.stream(UploadType.values())
                .map(uploadConfigService::getConfig)
                .toList();
        return ResponseEntity.ok(
                ApiResponseModel.<List<UploadConfigResponseDto>>builder()
                        .message("Get upload configs successfully")
                        .data(configs)
                        .build());
    }
}
