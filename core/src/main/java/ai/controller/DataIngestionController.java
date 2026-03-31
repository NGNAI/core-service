package ai.controller;

import java.util.Arrays;
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

import ai.dto.own.request.DataIngestionCreateFolderRequestDto;
import ai.dto.own.request.DataIngestionUpdateFolderRequestDto;
import ai.dto.own.request.DataIngestionUploadRequestDto;
import ai.dto.own.request.filter.DataIngestionFilterDto;
import ai.dto.own.response.DataIngestionDownloadData;
import ai.dto.own.response.DataIngestionJobStatusResponseDto;
import ai.dto.own.response.DataIngestionPresignedUrlResponseDto;
import ai.dto.own.response.DataIngestionResponseDto;
import ai.enums.DataScope;
import ai.model.ApiResponseModel;
import ai.service.DataIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Tag(name = "Data Ingestion", description = "Data ingestion management APIs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/data-ingestion")
@RestController
public class DataIngestionController {
        DataIngestionService dataIngestionService;

        @Operation(summary = "Get list of available data ingestion access levels", description = "Get list of available data ingestion access levels")
        @GetMapping("/level")
        ResponseEntity<ApiResponseModel<List<DataScope>>> accessLevel() {
                return ResponseEntity.ok(
                                ApiResponseModel.<List<DataScope>>builder()
                                                .message("Get data ingestion access levels successfully")
                                                .data(Arrays.asList(DataScope.values()))
                                                .build());
        }

        @Operation(summary = "Get details info by id (folder or file)", description = "Get detail of a single data ingestion item")
        @GetMapping("/{dataIngestionId}")
        ResponseEntity<ApiResponseModel<DataIngestionResponseDto>> get(@PathVariable UUID dataIngestionId) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DataIngestionResponseDto>builder()
                                                .message("Get data ingestion successfully")
                                                .data(dataIngestionService.getById(dataIngestionId))
                                                .build());
        }

        @Operation(summary = "Download data ingestion file", description = "Download file bytes from MinIO by data ingestion id")
        @GetMapping("/{dataIngestionId}/download")
        ResponseEntity<byte[]> download(@PathVariable UUID dataIngestionId) {
                DataIngestionDownloadData fileData = dataIngestionService.downloadById(dataIngestionId);

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

        @Operation(summary = "Get data ingestion download URL with file", description = "Get MinIO presigned URL for data ingestion file download")
        @GetMapping("/{dataIngestionId}/download-url")
        ResponseEntity<ApiResponseModel<DataIngestionPresignedUrlResponseDto>> getDownloadUrl(
                        @PathVariable UUID dataIngestionId,
                        @Parameter(description = "URL expiration in seconds, default 900", example = "900") @RequestParam(required = false) Integer expiresInSeconds) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DataIngestionPresignedUrlResponseDto>builder()
                                                .message("Get data ingestion download url successfully")
                                                .data(dataIngestionService.getPresignedDownloadUrl(dataIngestionId,
                                                                expiresInSeconds))
                                                .build());
        }

        @Operation(summary = "Upload data ingestion file", description = "Upload a data ingestion file to MinIO and optionally trigger ingestion")
        @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        ResponseEntity<ApiResponseModel<DataIngestionResponseDto>> upload(
                        @Valid @ModelAttribute DataIngestionUploadRequestDto requestDto) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DataIngestionResponseDto>builder()
                                                .message("Upload data ingestion successfully")
                                                .data(dataIngestionService.uploadDataIngestion(requestDto))
                                                .build());
        }

        @Operation(summary = "Create data ingestion folder", description = "Create a folder node in the data ingestion tree without file upload")
        @PostMapping("/folders")
        ResponseEntity<ApiResponseModel<DataIngestionResponseDto>> createFolder(
                        @Valid @RequestBody DataIngestionCreateFolderRequestDto requestDto) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DataIngestionResponseDto>builder()
                                                .message("Create data ingestion folder successfully")
                                                .data(dataIngestionService.createFolder(requestDto))
                                                .build());
        }

        @Operation(summary = "Get data ingestion list (folders and files)", description = "Get paginated list of data ingestion items with optional filters")
        @GetMapping
        ResponseEntity<ApiResponseModel<List<DataIngestionResponseDto>>> list(
                        @ModelAttribute DataIngestionFilterDto filterDto) {
                Page<DataIngestionResponseDto> page = dataIngestionService.getAll(filterDto);
                return ResponseEntity.ok(
                                ApiResponseModel.<List<DataIngestionResponseDto>>builder()
                                                .message("Get data ingestion list successfully")
                                                .count(page.getTotalElements())
                                                .data(page.getContent())
                                                .build());
        }

        @Operation(summary = "Rename or move folder", description = "Update folder name and/or move folder to another parent")
        @PutMapping("/folders/{dataIngestionId}")
        ResponseEntity<ApiResponseModel<DataIngestionResponseDto>> updateFolder(
                        @PathVariable UUID dataIngestionId,
                        @Valid @RequestBody DataIngestionUpdateFolderRequestDto requestDto) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DataIngestionResponseDto>builder()
                                                .message("Update data ingestion folder successfully")
                                                .data(dataIngestionService.updateFolder(dataIngestionId, requestDto))
                                                .build());
        }

        @Operation(summary = "Retry ingestion with failed data file", description = "Retry pushing failed data ingestion into ingestion pipeline")
        @PostMapping("/{dataIngestionId}/ingestion/retry")
        ResponseEntity<ApiResponseModel<DataIngestionResponseDto>> retryIngestion(
                        @PathVariable UUID dataIngestionId) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DataIngestionResponseDto>builder()
                                                .message("Retry data ingestion with failed data file successfully")
                                                .data(dataIngestionService.retryIngestion(dataIngestionId))
                                                .build());
        }

        @Operation(summary = "Get ingestion job status", description = "Poll ingestion processing status for a data ingestion item")
        @GetMapping("/{dataIngestionId}/ingestion/job-status")
        ResponseEntity<ApiResponseModel<DataIngestionJobStatusResponseDto>> ingestionJobStatus(
                        @PathVariable UUID dataIngestionId) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DataIngestionJobStatusResponseDto>builder()
                                                .message("Get data ingestion job status successfully")
                                                .data(dataIngestionService.pollIngestionJobStatus(dataIngestionId))
                                                .build());
        }

        @Operation(summary = "Delete data ingestion", description = "Delete a single data ingestion item by its ID")
        @DeleteMapping("/{dataIngestionId}")
        ResponseEntity<ApiResponseModel<DataIngestionResponseDto>> delete(@PathVariable UUID dataIngestionId) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DataIngestionResponseDto>builder()
                                                .message("Delete data ingestion successfully")
                                                .data(dataIngestionService.deleteById(dataIngestionId))
                                                .build());
        }

        @Operation(summary = "Delete folder", description = "Delete a single folder by its ID, and all its descendant data ingestion items will be deleted as well")
        @DeleteMapping("/folders/{dataIngestionId}")
        ResponseEntity<ApiResponseModel<Void>> deleteFolder(@PathVariable UUID dataIngestionId) {
                dataIngestionService.deleteFolderById(dataIngestionId);
                return ResponseEntity.ok(
                                ApiResponseModel.<Void>builder()
                                                .message("Delete data ingestion folder successfully")
                                                .build());
        }

}
