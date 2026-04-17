package ai.controller;

import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.response.AttachmentDownloadData;
import ai.dto.own.response.AttachmentPresignedUrlResponseDto;
import ai.dto.own.response.AttachmentResponseDto;
import ai.model.ApiResponseModel;
import ai.service.AttachmentService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Tag(name = "Attachment", description = "Conversation attachment management APIs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/attachment")
@RestController
@Hidden
public class AttachmentController {
    AttachmentService attachmentService;

    @Operation(summary = "Get attachment details", description = "Get detail of a single attachment item")
    @GetMapping("/{attachmentId}")
    ResponseEntity<ApiResponseModel<AttachmentResponseDto>> getDetails(@PathVariable UUID attachmentId) {
        return ResponseEntity.ok(
                ApiResponseModel.<AttachmentResponseDto>builder()
                        .message("Get attachment successfully")
                        .data(attachmentService.getDetails(attachmentId))
                        .build());
    }

    @Operation(summary = "Download attachment file", description = "Download attachment file bytes from MinIO by attachment id")
    @GetMapping("/{attachmentId}/download")
    ResponseEntity<byte[]> download(@PathVariable UUID attachmentId) {
        AttachmentDownloadData fileData = attachmentService.downloadById(attachmentId);

        HttpHeaders headers = new HttpHeaders();
        String contentType = (fileData.contentType() == null || fileData.contentType().isBlank())
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : fileData.contentType();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileData.fileName()).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileData.bytes());
    }

    @Operation(summary = "Get attachment download URL", description = "Get MinIO presigned URL for attachment file download")
    @GetMapping("/{attachmentId}/download-url")
    ResponseEntity<ApiResponseModel<AttachmentPresignedUrlResponseDto>> getDownloadUrl(
            @PathVariable UUID attachmentId,
            @Parameter(description = "URL expiration in seconds, default 900", example = "900") @RequestParam(required = false) Integer expiresInSeconds) {
        return ResponseEntity.ok(
                ApiResponseModel.<AttachmentPresignedUrlResponseDto>builder()
                        .message("Get attachment download url successfully")
                        .data(attachmentService.getPresignedDownloadUrl(attachmentId, expiresInSeconds))
                        .build());
    }
}
