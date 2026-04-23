package ai.controller;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.dto.own.response.SystemEventContractResponseDto;
import ai.enums.SystemEventSource;
import ai.enums.SystemEventType;
import ai.model.ApiResponseModel;
import ai.service.SystemEventSseService;
import ai.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Tag(name = "System Events", description = "System-wide realtime event APIs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/events")
@RestController
public class SystemEventController {
    SystemEventSseService systemEventSseService;

    @Operation(summary = "Get system event contract", description = "Danh sách chuẩn event type/source để FE và BE dùng chung một contract cố định")
    @GetMapping("/contract")
    ResponseEntity<ApiResponseModel<SystemEventContractResponseDto>> contract() {
        SystemEventContractResponseDto contract = SystemEventContractResponseDto.builder()
                .types(Arrays.asList(SystemEventType.values()))
                .sources(Arrays.asList(SystemEventSource.values()))
                .build();

        return ResponseEntity.ok(
                ApiResponseModel.<SystemEventContractResponseDto>builder()
                        .message("Get system event contract successfully")
                        .data(contract)
                        .build());
    }

    @CrossOrigin
    @Operation(summary = "Subscribe system events by SSE", description = "FE mở một kênh SSE chung theo orgId và userId để nhận sự kiện realtime toàn hệ thống")
    @GetMapping(value = "/sse/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter subscribe() {
        UUID tokenOrgId = JwtUtil.getOrgId();
        UUID tokenUserId = JwtUtil.getUserId();
        return systemEventSseService.subscribe(tokenOrgId, tokenUserId);
    }
}