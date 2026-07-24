package ai.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.request.ShareLinkCreateRequestDto;
import ai.dto.own.request.filter.ShareLinkFilterDto;
import ai.dto.own.response.ShareLinkResponseDto;
import ai.dto.own.response.ShareLinkStatsDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.ShareLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * API quản lý share link cho owner (user đã đăng nhập).
 * Owner tạo/list/revoke/stats share link cho Topic/Notebook của mình.
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/share")
@Tag(name = "Share Link", description = "API quản lý link chia sẻ public cho Topic/Notebook (owner)")
@RestController
public class ShareUserController {
    ShareLinkService shareLinkService;

    @Operation(summary = "Tạo share link", description = "Tạo link chia sẻ public cho Topic/Notebook. Viewer chỉ có quyền read-only. Có thể đặt password và expiry tùy chọn.")
    @PostMapping
    ResponseEntity<ApiResponseModel<ShareLinkResponseDto>> create(@Valid @RequestBody ShareLinkCreateRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<ShareLinkResponseDto>builder()
                        .message("Tạo link chia sẻ thành công")
                        .data(shareLinkService.create(requestDto))
                        .build());
    }

    @Operation(summary = "List share link đã tạo", description = "Lấy danh sách share link của user hiện tại, có filter theo resourceType/resourceId/activeOnly")
    @GetMapping
    ResponseEntity<ApiResponseModel<List<ShareLinkResponseDto>>> list(@Valid @ModelAttribute ShareLinkFilterDto filterDto) {
        CustomPairModel<Long, List<ShareLinkResponseDto>> result = shareLinkService.list(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<ShareLinkResponseDto>>builder()
                        .message("Lấy danh sách link chia sẻ thành công")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build());
    }

    @Operation(summary = "Revoke (hủy) share link", description = "Hủy link chia sẻ, viewer không thể truy cập nữa. Chỉ owner mới được hủy.")
    @DeleteMapping("/{linkId}")
    ResponseEntity<ApiResponseModel<Void>> revoke(@PathVariable UUID linkId) {
        shareLinkService.revoke(linkId);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Hủy link chia sẻ thành công")
                        .build());
    }

    @Operation(summary = "Thống kê lượt xem", description = "Lấy thống kê view count của share link. Chỉ owner.")
    @GetMapping("/{linkId}/stats")
    ResponseEntity<ApiResponseModel<ShareLinkStatsDto>> getStats(@PathVariable UUID linkId) {
        return ResponseEntity.ok(
                ApiResponseModel.<ShareLinkStatsDto>builder()
                        .message("Lấy thống kê link chia sẻ thành công")
                        .data(shareLinkService.getStats(linkId))
                        .build());
    }
}