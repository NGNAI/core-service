package ai.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.request.MediaCreateFolderRequestDto;
import ai.dto.own.request.MediaUpdateFolderRequestDto;
import ai.dto.own.request.MediaUploadRequestDto;
import ai.dto.own.request.filter.MediaFilterDto;
import ai.dto.own.response.MediaDownloadData;
import ai.dto.own.response.MediaJobStatusResponseDto;
import ai.dto.own.response.MediaPresignedUrlResponseDto;
import ai.dto.own.response.MediaResponseDto;
import ai.model.ApiResponseModel;
import ai.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Tag(name = "Media", description = "Media management APIs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/media")
@RestController
public class MediaController {
    MediaService mediaService;

    @Operation(summary = "Get media by id", description = "Get detail of a single media item")
    @GetMapping("/{mediaId}")
    ResponseEntity<ApiResponseModel<MediaResponseDto>> get(@PathVariable UUID mediaId) {
        return ResponseEntity.ok(
                ApiResponseModel.<MediaResponseDto>builder()
                        .message("Get media successfully")
                        .data(mediaService.getById(mediaId))
                        .build());
    }

    @Operation(summary = "Download media file", description = "Download file bytes from MinIO by media id")
    @GetMapping("/{mediaId}/download")
    ResponseEntity<byte[]> download(@PathVariable UUID mediaId) {
        MediaDownloadData fileData = mediaService.downloadById(mediaId);

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

    @Operation(summary = "Get media download URL", description = "Get MinIO presigned URL for file download")
    @GetMapping("/{mediaId}/download-url")
    ResponseEntity<ApiResponseModel<MediaPresignedUrlResponseDto>> getDownloadUrl(
            @PathVariable UUID mediaId,
            @Parameter(description = "URL expiration in seconds, default 900", example = "900") @RequestParam(required = false) Integer expiresInSeconds) {
        return ResponseEntity.ok(
                ApiResponseModel.<MediaPresignedUrlResponseDto>builder()
                        .message("Get media download url successfully")
                        .data(mediaService.getPresignedDownloadUrl(mediaId, expiresInSeconds))
                        .build());
    }

    @Operation(summary = "Upload media", description = "Upload media file to MinIO and optionally trigger ingestion")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponseModel<MediaResponseDto>> upload(@Valid @ModelAttribute MediaUploadRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<MediaResponseDto>builder()
                        .message("Upload media successfully")
                        .data(mediaService.uploadMedia(requestDto))
                        .build());
    }

    @Operation(summary = "Create media folder", description = "Create folder node in media tree without file upload")
    @PostMapping("/folders")
    ResponseEntity<ApiResponseModel<MediaResponseDto>> createFolder(
            @Valid @RequestBody MediaCreateFolderRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<MediaResponseDto>builder()
                        .message("Create media folder successfully")
                        .data(mediaService.createFolder(requestDto))
                        .build());
    }

    @Operation(summary = "Get media list with paging", description = "Return media items with paging metadata: pageNumber, pageSize, totalPages, totalElements")
    @GetMapping
    ResponseEntity<ApiResponseModel<List<MediaResponseDto>>> list(@ModelAttribute MediaFilterDto filterDto) {
        Page<MediaResponseDto> page = mediaService.getAll(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<MediaResponseDto>>builder()
                        .message("Get media list successfully")
                        .count(page.getTotalElements())
                        .data(page.getContent())
                        .build());
    }

    @Operation(summary = "Rename or move folder", description = "Update folder name and/or move folder to another parent")
    @PutMapping("/folders/{mediaId}")
    ResponseEntity<ApiResponseModel<MediaResponseDto>> updateFolder(
            @PathVariable UUID mediaId,
            @Valid @RequestBody MediaUpdateFolderRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<MediaResponseDto>builder()
                        .message("Update media folder successfully")
                        .data(mediaService.updateFolder(mediaId, requestDto))
                        .build());
    }

    @Operation(summary = "Retry ingestion", description = "Retry pushing failed ingestion media into ingestion pipeline")
    @PostMapping("/{mediaId}/ingestion/retry")
    ResponseEntity<ApiResponseModel<MediaResponseDto>> retryIngestion(
            @PathVariable UUID mediaId) {
        return ResponseEntity.ok(
                ApiResponseModel.<MediaResponseDto>builder()
                        .message("Retry media ingestion successfully")
                        .data(mediaService.retryIngestion(mediaId))
                        .build());
    }

    @Operation(summary = "Get ingestion job status", description = "Poll ingestion processing status by jobId")
    @GetMapping("/{mediaId}/ingestion/job-status")
    ResponseEntity<ApiResponseModel<MediaJobStatusResponseDto>> ingestionJobStatus(@PathVariable UUID mediaId) {
        return ResponseEntity.ok(
                ApiResponseModel.<MediaJobStatusResponseDto>builder()
                        .message("Get media ingestion status successfully")
                        .data(mediaService.pollIngestionJobStatus(mediaId))
                        .build());
    }

    @Operation(summary = "Delete media by id", description = "Delete a single media item by its ID")
    @DeleteMapping("/{mediaId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID mediaId) {
        mediaService.deleteById(mediaId);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete media successfully")
                        .build());
    }
}
