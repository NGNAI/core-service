package ai.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import ai.dto.own.request.MediaRetryIngestionRequestDto;
import ai.dto.own.request.MediaUpdateFolderRequestDto;
import ai.dto.own.request.MediaUploadRequestDto;
import ai.dto.own.response.MediaJobStatusResponseDto;
import ai.dto.own.response.MediaPageResponseDto;
import ai.dto.own.response.MediaPresignedUrlResponseDto;
import ai.dto.own.response.MediaResponseDto;
import ai.dto.own.response.MediaUploadResponseDto;
import ai.enums.MediaUploadTarget;
import ai.model.ApiResponseModel;
import ai.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Tag(name = "Media", description = "Media management APIs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/prv/media")
@RestController
public class MediaController {
    MediaService mediaService;

    @Operation(summary = "Get media by id", description = "Get detail of a single media item")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Media detail returned successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "mediaDetailResponse",
                    value = "{\n" +
                        "  \"status\": 1000,\n" +
                        "  \"message\": \"Get media successfully\",\n" +
                        "  \"data\": {\n" +
                        "    \"id\": \"11111111-1111-1111-1111-111111111111\",\n" +
                        "    \"name\": \"policy.pdf\",\n" +
                        "    \"type\": \"pdf\",\n" +
                        "    \"size\": 20480,\n" +
                        "    \"minioPath\": \"hr/2026/3/uuid-policy.pdf\",\n" +
                        "    \"parentId\": null,\n" +
                        "    \"ownerId\": \"22222222-2222-2222-2222-222222222222\",\n" +
                        "    \"orgId\": \"33333333-3333-3333-3333-333333333333\",\n" +
                        "    \"accessLevel\": \"PRIVATE\",\n" +
                        "    \"jobId\": \"44444444-4444-4444-4444-444444444444\",\n" +
                        "    \"ingestionStatus\": \"PENDING\",\n" +
                        "    \"downloadCount\": 0,\n" +
                        "    \"createdAt\": \"2026-03-18T08:10:00Z\",\n" +
                        "    \"updatedAt\": \"2026-03-18T08:10:00Z\",\n" +
                        "    \"target\": \"INGESTION\"\n" +
                        "  }\n" +
                        "}"
                )
            )
        )
    })
    @GetMapping("/{mediaId}")
    ResponseEntity<ApiResponseModel<MediaResponseDto>> getById(@PathVariable UUID mediaId) {
        return ResponseEntity.ok(
                ApiResponseModel.<MediaResponseDto>builder()
                        .message("Get media successfully")
                        .data(mediaService.getById(mediaId))
                        .build()
        );
    }

        @Operation(summary = "Download media file", description = "Download file bytes from MinIO by media id")
        @GetMapping("/{mediaId}/download")
        ResponseEntity<byte[]> download(@PathVariable UUID mediaId) {
        MediaService.MediaDownloadData fileData = mediaService.downloadById(mediaId);

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
            @Parameter(description = "URL expiration in seconds, default 900", example = "900")
            @RequestParam(required = false) Integer expiresInSeconds
        ) {
        return ResponseEntity.ok(
            ApiResponseModel.<MediaPresignedUrlResponseDto>builder()
                .message("Get media download url successfully")
                .data(mediaService.getPresignedDownloadUrl(mediaId, expiresInSeconds))
                .build()
        );
        }

        @Operation(summary = "Upload media", description = "Upload media file to MinIO and optionally trigger ingestion")
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200",
                description = "Media uploaded successfully",
                content = @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(
                            name = "uploadIngestionResponse",
                            value = "{\n" +
                                "  \"status\": 1000,\n" +
                                "  \"message\": \"Upload media successfully\",\n" +
                                "  \"data\": {\n" +
                                "    \"mediaId\": \"11111111-1111-1111-1111-111111111111\",\n" +
                                "    \"minioPath\": \"hr/2026/3/uuid-policy.pdf\",\n" +
                                "    \"target\": \"INGESTION\",\n" +
                                "    \"ingestionStatus\": \"PENDING\",\n" +
                                "    \"jobId\": \"44444444-4444-4444-4444-444444444444\"\n" +
                                "  }\n" +
                                "}"
                        ),
                        @ExampleObject(
                            name = "uploadAvatarResponse",
                            value = "{\n" +
                                "  \"status\": 1000,\n" +
                                "  \"message\": \"Upload media successfully\",\n" +
                                "  \"data\": {\n" +
                                "    \"mediaId\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                                "    \"minioPath\": \"avatar/2026/3/uuid-avatar.png\",\n" +
                                "    \"target\": \"AVATAR\",\n" +
                                "    \"ingestionStatus\": \"NONE\",\n" +
                                "    \"jobId\": null\n" +
                                "  }\n" +
                                "}"
                        )
                    }
                )
            )
        })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponseModel<MediaUploadResponseDto>> upload(@ModelAttribute MediaUploadRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<MediaUploadResponseDto>builder()
                        .message("Upload media successfully")
                        .data(mediaService.uploadMedia(requestDto))
                        .build()
        );
    }

        @Operation(
            summary = "Create media folder",
            description = "Create folder node in media tree without file upload",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "Folder payload",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "createFolderRequest",
                        value = "{\n" +
                            "  \"name\": \"HR Documents\",\n" +
                            "  \"parentId\": null,\n" +
                            "  \"ownerId\": \"22222222-2222-2222-2222-222222222222\",\n" +
                            "  \"orgId\": \"33333333-3333-3333-3333-333333333333\",\n" +
                            "  \"visibility\": \"PRIVATE\",\n" +
                            "  \"target\": \"INGESTION\"\n" +
                            "}"
                    )
                )
            )
        )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Folder created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "createFolderResponse",
                                    value = "{\n" +
                                            "  \"status\": 1000,\n" +
                                            "  \"message\": \"Create media folder successfully\",\n" +
                                            "  \"data\": {\n" +
                                            "    \"id\": \"55555555-5555-5555-5555-555555555555\",\n" +
                                            "    \"name\": \"HR Documents\",\n" +
                                            "    \"type\": \"FOLDER\",\n" +
                                            "    \"size\": 0,\n" +
                                            "    \"minioPath\": null,\n" +
                                            "    \"parentId\": null,\n" +
                                            "    \"ownerId\": \"22222222-2222-2222-2222-222222222222\",\n" +
                                            "    \"orgId\": \"33333333-3333-3333-3333-333333333333\",\n" +
                                            "    \"accessLevel\": \"PRIVATE\",\n" +
                                            "    \"jobId\": null,\n" +
                                            "    \"ingestionStatus\": \"NONE\",\n" +
                                            "    \"downloadCount\": 0,\n" +
                                            "    \"createdAt\": \"2026-03-18T08:15:00Z\",\n" +
                                            "    \"updatedAt\": \"2026-03-18T08:15:00Z\",\n" +
                                            "    \"target\": \"INGESTION\"\n" +
                                            "  }\n" +
                                            "}"
                            )
                    )
            )
    })
    @PostMapping("/folders")
    ResponseEntity<ApiResponseModel<MediaResponseDto>> createFolder(@RequestBody MediaCreateFolderRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<MediaResponseDto>builder()
                        .message("Create media folder successfully")
                        .data(mediaService.createFolder(requestDto))
                        .build()
        );
    }

    @Operation(
            summary = "Get paged media list",
            description = "Return media items with paging metadata: pageNumber, pageSize, totalPages, totalElements"
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200",
                description = "Media list returned successfully",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "pagedMediaResponse",
                        value = "{\n" +
                            "  \"status\": 1000,\n" +
                            "  \"message\": \"Get media list successfully\",\n" +
                            "  \"data\": {\n" +
                            "    \"items\": [\n" +
                            "      {\n" +
                            "        \"id\": \"11111111-1111-1111-1111-111111111111\",\n" +
                            "        \"name\": \"policy.pdf\",\n" +
                            "        \"type\": \"pdf\",\n" +
                            "        \"size\": 20480,\n" +
                            "        \"minioPath\": \"hr/2026/3/uuid-policy.pdf\",\n" +
                            "        \"parentId\": null,\n" +
                            "        \"ownerId\": \"22222222-2222-2222-2222-222222222222\",\n" +
                            "        \"orgId\": \"33333333-3333-3333-3333-333333333333\",\n" +
                            "        \"accessLevel\": \"PRIVATE\",\n" +
                            "        \"jobId\": \"44444444-4444-4444-4444-444444444444\",\n" +
                            "        \"ingestionStatus\": \"PENDING\",\n" +
                            "        \"downloadCount\": 0,\n" +
                            "        \"createdAt\": \"2026-03-18T08:10:00Z\",\n" +
                            "        \"updatedAt\": \"2026-03-18T08:10:00Z\",\n" +
                            "        \"target\": \"INGESTION\"\n" +
                            "      }\n" +
                            "    ],\n" +
                            "    \"pageNumber\": 0,\n" +
                            "    \"pageSize\": 10,\n" +
                            "    \"totalPages\": 5,\n" +
                            "    \"totalElements\": 42\n" +
                            "  }\n" +
                            "}"
                    )
                )
            )
    })
    @GetMapping
    ResponseEntity<ApiResponseModel<MediaPageResponseDto>> list(
            @Parameter(description = "Organization id", required = true)
            @RequestParam UUID orgId,
            @Parameter(description = "Owner id filter")
            @RequestParam(required = false) UUID ownerId,
            @Parameter(description = "Parent folder id filter, null means root")
            @RequestParam(required = false) UUID parentId,
            @Parameter(description = "Media target filter: INGESTION or AVATAR")
            @RequestParam(required = false) MediaUploadTarget target,
            @Parameter(description = "Page index, 0-based", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "Sort field whitelist: name,type,size,createdAt,updatedAt,downloadCount,ingestionStatus,accessLevel", example = "createdAt")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction: ASC or DESC", example = "DESC")
            @RequestParam(required = false, defaultValue = "ASC") String sortDir
    ) {
        Page<MediaResponseDto> page = mediaService.listMedia(orgId, ownerId, parentId, target, pageNumber, pageSize, sortBy, sortDir);
        MediaPageResponseDto data = MediaPageResponseDto.builder()
            .items(page.getContent())
            .pageNumber(page.getNumber())
            .pageSize(page.getSize())
            .totalPages(page.getTotalPages())
            .totalElements(page.getTotalElements())
            .build();

        return ResponseEntity.ok(
            ApiResponseModel.<MediaPageResponseDto>builder()
                        .message("Get media list successfully")
                        .data(data)
                        .build()
        );
    }

        @Operation(
            summary = "Rename or move folder",
            description = "Update folder name and/or move folder to another parent",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "Folder update payload",
                content = @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(
                            name = "renameFolderRequest",
                            value = "{\n" +
                                "  \"name\": \"Human Resource Documents\"\n" +
                                "}"
                        ),
                        @ExampleObject(
                            name = "moveFolderRequest",
                            value = "{\n" +
                                "  \"parentId\": \"66666666-6666-6666-6666-666666666666\"\n" +
                                "}"
                        ),
                        @ExampleObject(
                            name = "moveFolderToRootRequest",
                            value = "{\n" +
                                "  \"moveToRoot\": true\n" +
                                "}"
                        )
                    }
                )
            )
        )
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200",
                description = "Folder updated successfully",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "updateFolderResponse",
                        value = "{\n" +
                            "  \"status\": 1000,\n" +
                            "  \"message\": \"Update media folder successfully\",\n" +
                            "  \"data\": {\n" +
                            "    \"id\": \"55555555-5555-5555-5555-555555555555\",\n" +
                            "    \"name\": \"Human Resource Documents\",\n" +
                            "    \"type\": \"FOLDER\",\n" +
                            "    \"size\": 0,\n" +
                            "    \"minioPath\": null,\n" +
                            "    \"parentId\": \"66666666-6666-6666-6666-666666666666\",\n" +
                            "    \"ownerId\": \"22222222-2222-2222-2222-222222222222\",\n" +
                            "    \"orgId\": \"33333333-3333-3333-3333-333333333333\",\n" +
                            "    \"accessLevel\": \"PRIVATE\",\n" +
                            "    \"jobId\": null,\n" +
                            "    \"ingestionStatus\": \"NONE\",\n" +
                            "    \"downloadCount\": 0,\n" +
                            "    \"createdAt\": \"2026-03-18T08:15:00Z\",\n" +
                            "    \"updatedAt\": \"2026-03-18T08:20:00Z\",\n" +
                            "    \"target\": \"INGESTION\"\n" +
                            "  }\n" +
                            "}"
                    )
                )
            )
        })
    @PutMapping("/folders/{mediaId}")
    ResponseEntity<ApiResponseModel<MediaResponseDto>> updateFolder(
            @PathVariable UUID mediaId,
            @RequestBody MediaUpdateFolderRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                ApiResponseModel.<MediaResponseDto>builder()
                        .message("Update media folder successfully")
                        .data(mediaService.updateFolder(mediaId, requestDto))
                        .build()
        );
    }

        @Operation(
            summary = "Retry ingestion",
            description = "Retry pushing failed ingestion media into ingestion pipeline",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "Retry payload",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "retryIngestionRequest",
                        value = "{\n" +
                            "  \"username\": \"admin.user\",\n" +
                            "  \"unit\": \"hr\",\n" +
                            "  \"visibility\": \"PRIVATE\"\n" +
                            "}"
                    )
                )
            )
        )
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200",
                description = "Retry submitted successfully",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "retryIngestionResponse",
                        value = "{\n" +
                            "  \"status\": 1000,\n" +
                            "  \"message\": \"Retry media ingestion successfully\",\n" +
                            "  \"data\": {\n" +
                            "    \"mediaId\": \"11111111-1111-1111-1111-111111111111\",\n" +
                            "    \"minioPath\": \"hr/2026/3/uuid-policy.pdf\",\n" +
                            "    \"target\": \"INGESTION\",\n" +
                            "    \"ingestionStatus\": \"PENDING\",\n" +
                            "    \"jobId\": \"77777777-7777-7777-7777-777777777777\"\n" +
                            "  }\n" +
                            "}"
                    )
                )
            )
        })
        @PostMapping("/{mediaId}/ingestion/retry")
        ResponseEntity<ApiResponseModel<MediaUploadResponseDto>> retryIngestion(
            @PathVariable UUID mediaId,
            @RequestBody MediaRetryIngestionRequestDto requestDto
        ) {
        return ResponseEntity.ok(
            ApiResponseModel.<MediaUploadResponseDto>builder()
                .message("Retry media ingestion successfully")
                .data(mediaService.retryIngestion(mediaId, requestDto))
                .build()
        );
        }

        @Operation(summary = "Get ingestion job status", description = "Poll ingestion processing status by jobId")
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200",
                description = "Ingestion job status returned successfully",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "ingestionJobStatusResponse",
                        value = "{\n" +
                            "  \"status\": 1000,\n" +
                            "  \"message\": \"Get media ingestion status successfully\",\n" +
                            "  \"data\": {\n" +
                            "    \"mediaId\": \"11111111-1111-1111-1111-111111111111\",\n" +
                            "    \"jobId\": \"77777777-7777-7777-7777-777777777777\",\n" +
                            "    \"ingestionStatus\": \"COMPLETED\",\n" +
                            "    \"message\": \"completed\"\n" +
                            "  }\n" +
                            "}"
                    )
                )
            )
        })
    @GetMapping("/jobs/{jobId}/ingestion-status")
    ResponseEntity<ApiResponseModel<MediaJobStatusResponseDto>> getIngestionJobStatus(@PathVariable UUID jobId) {
        return ResponseEntity.ok(
                ApiResponseModel.<MediaJobStatusResponseDto>builder()
                        .message("Get media ingestion status successfully")
                        .data(mediaService.pollIngestionJobStatus(jobId))
                        .build()
        );
    }
}
