package ai.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.response.MessageResponseDto;
import ai.dto.own.response.SharedResourceResponseDto;
import ai.entity.postgres.ShareLinkEntity;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.security.ShareLinkAuthFilter;
import ai.service.ShareLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * API public cho viewer truy cập share link (read-only, không cần JWT).
 * <p>
 * <b>Flow xác thực (2 bước):</b>
 * <ol>
 *   <li>
 *     <b>Bước 1 — Get metadata:</b><br>
 *     Gọi {@code GET /public/share/{token}} để lấy thông tin resource.
 *     Endpoint này <b>không yêu cầu password</b>, chỉ check token còn hiệu lực (không revoked/expired).
 *     Response chứa field {@code passwordRequired} — nếu {@code true}, FE cần hiển thị dialog nhập mật khẩu.
 *   </li>
 *   <li>
 *     <b>Bước 2 — Truy cập dữ liệu:</b><br>
 *     Các endpoint {@code /messages}, {@code /sources}, {@code /download-url}
 *     <b>yêu cầu password</b> (nếu link có password). Gửi password qua header
 *     <code>X-Share-Password: &lt;password&gt;</code> trên mỗi request.
 *   </li>
 * </ol>
 * <p>
 * Nếu thiếu hoặc sai password → HTTP 403 với error code tương ứng:
 * <ul>
 *   <li>{@code SHARE_LINK_PASSWORD_REQUIRED (1177)} — chưa gửi password</li>
 *   <li>{@code SHARE_LINK_PASSWORD_INVALID (1178)} — password sai</li>
 * </ul>
 * <p>
 * Auth: {@link ai.security.ShareLinkAuthFilter} xác thực token + password (nếu có),
 * put {@link ShareLinkEntity} vào request attribute {@code shareLink}.
 * Controller lấy entity đó để phục vụ dữ liệu read-only.
 * <p>
 * Viewer chỉ được: xem metadata, list messages, list sources, lấy presigned download URL.
 * Viewer KHÔNG được: chat, upload source, feedback, edit, delete.
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/public/share")
@Tag(name = "Share Link Public", description = "API truy cập share link public (read-only, không cần JWT). Xem class Javadoc để biết flow xác thực chi tiết.")
@RestController
public class SharePublicController {
    ShareLinkService shareLinkService;

    @Operation(summary = "Get shared resource metadata", description = "Bước 1 trong flow xác thực. Lấy metadata Topic/Notebook được share. KHÔNG yêu cầu password — response chứa field passwordRequired để FE biết có cần hiển thị dialog nhập mật khẩu hay không. Không lộ ownerId/orgId.")
    @GetMapping("/{token}")
    ResponseEntity<ApiResponseModel<SharedResourceResponseDto>> getSharedResource(
            @PathVariable String token,
            @RequestAttribute(ShareLinkAuthFilter.SHARE_LINK_ATTR) ShareLinkEntity link) {
        return ResponseEntity.ok(
                ApiResponseModel.<SharedResourceResponseDto>builder()
                        .message("Lấy thông tin tài nguyên chia sẻ thành công")
                        .data(shareLinkService.getSharedResource(link))
                        .build());
    }

    @Operation(summary = "Get shared messages", description = "Bước 2. Lấy danh sách messages của Topic/Notebook được share (read-only, phân trang). Yêu cầu header X-Share-Password nếu link có password.")
    @GetMapping("/{token}/messages")
    ResponseEntity<ApiResponseModel<List<MessageResponseDto>>> getSharedMessages(
            @PathVariable String token,
            @RequestAttribute(ShareLinkAuthFilter.SHARE_LINK_ATTR) ShareLinkEntity link,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        CustomPairModel<Long, List<MessageResponseDto>> result = shareLinkService.getSharedMessages(link, page, size);
        return ResponseEntity.ok(
                ApiResponseModel.<List<MessageResponseDto>>builder()
                        .message("Lấy danh sách tin nhắn chia sẻ thành công")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build());
    }

    @Operation(summary = "Get shared sources", description = "Lấy danh sách sources của Topic/Notebook được share (read-only, phân trang). Yêu cầu header X-Share-Password nếu link có password.")
    @GetMapping("/{token}/sources")
    ResponseEntity<ApiResponseModel<Object>> getSharedSources(
            @PathVariable String token,
            @RequestAttribute(ShareLinkAuthFilter.SHARE_LINK_ATTR) ShareLinkEntity link,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Object result = shareLinkService.getSharedSources(link, page, size);
        return ResponseEntity.ok(
                ApiResponseModel.<Object>builder()
                        .message("Lấy danh sách nguồn chia sẻ thành công")
                        .data(result)
                        .build());
    }

    @Operation(summary = "Get shared source download URL", description = "Lấy presigned download URL cho một source (read-only). URL có thời hạn. Yêu cầu header X-Share-Password nếu link có password.")
    @GetMapping("/{token}/sources/{sourceId}/download-url")
    ResponseEntity<ApiResponseModel<Object>> getSharedSourceDownloadUrl(
            @PathVariable String token,
            @PathVariable UUID sourceId,
            @RequestAttribute(ShareLinkAuthFilter.SHARE_LINK_ATTR) ShareLinkEntity link,
            @RequestParam(required = false) Integer expiresInSeconds) {
        Object result = shareLinkService.getSharedSourceDownloadUrl(link, sourceId, expiresInSeconds);
        return ResponseEntity.ok(
                ApiResponseModel.<Object>builder()
                        .message("Lấy URL tải xuống thành công")
                        .data(result)
                        .build());
    }
}