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

import ai.dto.own.request.DriveCreateFolderRequestDto;
import ai.dto.own.request.DriveUpdateFolderRequestDto;
import ai.dto.own.request.DriveUploadRequestDto;
import ai.dto.own.request.filter.DriveFilterDto;
import ai.dto.own.response.DriveDownloadData;
import ai.dto.own.response.DrivePresignedUrlResponseDto;
import ai.dto.own.response.DriveResponseDto;
import ai.dto.own.response.DriveTreeResponseDto;
import ai.model.ApiResponseModel;
import ai.service.DriveService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Tag(name = "Drive", description = "Personal drive management APIs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/drive")
@RestController
@Hidden
public class DriveController {
    DriveService driveService;

    @Operation(summary = "Get full drive tree", description = "Get full tree of personal drive folders and files")
    @GetMapping("/get-info")
    ResponseEntity<ApiResponseModel<List<DriveTreeResponseDto>>> getInfo() {
        List<DriveTreeResponseDto> tree = driveService.getInfo();
        return ResponseEntity.ok(
                ApiResponseModel.<List<DriveTreeResponseDto>>builder()
                        .message("Get drive info successfully")
                        .count((long) tree.size())
                        .data(tree)
                        .build());
    }

    @Operation(summary = "Get drive details by id", description = "Get detail of a single drive folder or file")
    @GetMapping("/{driveId}")
    ResponseEntity<ApiResponseModel<DriveResponseDto>> get(@PathVariable UUID driveId) {
        return ResponseEntity.ok(
                ApiResponseModel.<DriveResponseDto>builder()
                        .message("Get drive successfully")
                        .data(driveService.getById(driveId))
                        .build());
    }

    @Operation(summary = "Download drive file", description = "Download file bytes from MinIO by drive id")
    @GetMapping("/{driveId}/download")
    ResponseEntity<byte[]> download(@PathVariable UUID driveId) {
        DriveDownloadData fileData = driveService.downloadById(driveId);

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

    @Operation(summary = "Get drive file presigned URL", description = "Get MinIO presigned URL for drive file download")
    @GetMapping("/{driveId}/download-url")
    ResponseEntity<ApiResponseModel<DrivePresignedUrlResponseDto>> getDownloadUrl(
            @PathVariable UUID driveId,
            @Parameter(description = "URL expiration in seconds, default 900", example = "900") @RequestParam(required = false) Integer expiresInSeconds) {
        return ResponseEntity.ok(
                ApiResponseModel.<DrivePresignedUrlResponseDto>builder()
                        .message("Get drive download url successfully")
                        .data(driveService.getPresignedDownloadUrl(driveId, expiresInSeconds))
                        .build());
    }

    @Operation(summary = "Upload drive file", description = "Upload a file to personal drive on MinIO")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponseModel<DriveResponseDto>> upload(@Valid @ModelAttribute DriveUploadRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<DriveResponseDto>builder()
                        .message("Upload drive file successfully")
                        .data(driveService.upload(requestDto))
                        .build());
    }

    @Operation(summary = "Create drive folder", description = "Create folder in personal drive")
    @PostMapping("/folders")
    ResponseEntity<ApiResponseModel<DriveResponseDto>> createFolder(
            @Valid @RequestBody DriveCreateFolderRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<DriveResponseDto>builder()
                        .message("Create drive folder successfully")
                        .data(driveService.createFolder(requestDto))
                        .build());
    }

    @Operation(summary = "Get drive list", description = "Get paginated list of drive folders and files")
    @GetMapping
    ResponseEntity<ApiResponseModel<List<DriveResponseDto>>> list(@ModelAttribute DriveFilterDto filterDto) {
        Page<DriveResponseDto> page = driveService.getAll(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<DriveResponseDto>>builder()
                        .message("Get drive list successfully")
                        .count(page.getTotalElements())
                        .data(page.getContent())
                        .build());
    }

    @Operation(summary = "Rename or move drive folder", description = "Update folder name and or move folder to another parent")
    @PutMapping("/folders/{driveId}")
    ResponseEntity<ApiResponseModel<DriveResponseDto>> updateFolder(
            @PathVariable UUID driveId,
            @Valid @RequestBody DriveUpdateFolderRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<DriveResponseDto>builder()
                        .message("Update drive folder successfully")
                        .data(driveService.updateFolder(driveId, requestDto))
                        .build());
    }

    @Operation(summary = "Delete drive file", description = "Delete a single drive file by id")
    @DeleteMapping("/{driveId}")
    ResponseEntity<ApiResponseModel<DriveResponseDto>> delete(@PathVariable UUID driveId) {
        return ResponseEntity.ok(
                ApiResponseModel.<DriveResponseDto>builder()
                        .message("Delete drive file successfully")
                        .data(driveService.deleteById(driveId))
                        .build());
    }

    @Operation(summary = "Delete drive folder", description = "Delete folder and all descendants in personal drive")
    @DeleteMapping("/folders/{driveId}")
    ResponseEntity<ApiResponseModel<Void>> deleteFolder(@PathVariable UUID driveId) {
        driveService.deleteFolderById(driveId);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete drive folder successfully")
                        .build());
    }
}
