package ai.controller.user;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import ai.constant.InputValidateKey;
import ai.dto.outer.ingestion.response.IngestionStatusResponseDto;
import ai.dto.own.request.DataIngestionCreateFolderRequestDto;
import ai.dto.own.request.DataIngestionUpdateFolderRequestDto;
import ai.dto.own.request.DataIngestionUploadRequestDto;
import ai.dto.own.request.filter.DataIngestionFilterDto;
import ai.dto.own.request.filter.OrganizationFilterDto;
import ai.dto.own.response.DataIngestionDownloadData;
import ai.dto.own.response.DataIngestionJobStatusResponseDto;
import ai.dto.own.response.DataIngestionPresignedUrlResponseDto;
import ai.dto.own.response.DataIngestionResponseDto;
import ai.dto.own.response.OrganizationResponseDto;
import ai.entity.postgres.DataIngestionEntity;
import ai.enums.DataScope;
import ai.enums.DataSource;
import ai.enums.IngestionStatus;
import ai.enums.PermissionAction;
import ai.enums.PermissionResource;
import ai.enums.PermissionScope;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.model.PermissionGrantModel;
import ai.service.DataIngestionService;
import ai.service.OrganizationService;
import ai.service.OrganizationUserRoleService;
import ai.service.PermissionCheckerService;
import ai.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Tag(name = "Data Ingestion", description = "Data ingestion management APIs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/data-ingestion")
@RestController
public class DataIngestionUserController {
        DataIngestionService dataIngestionService;
        OrganizationUserRoleService ourService;
        PermissionCheckerService permissionCheckerService;
        OrganizationService organizationService;

        @Operation(summary = "Get list of available data ingestion access levels", description = "Get list of available data ingestion access levels")
        @GetMapping("/level")
        ResponseEntity<ApiResponseModel<List<DataScope>>> accessLevel() {
                return ResponseEntity.ok(
                                ApiResponseModel.<List<DataScope>>builder()
                                                .message("Get data ingestion access levels successfully")
                                                .data(Arrays.asList(DataScope.values()))
                                                .build());
        }

        @Operation(summary = "Get list of available data ingestion sources", description = "Get list of available data ingestion sources")
        @GetMapping("/source")
        ResponseEntity<ApiResponseModel<List<DataSource>>> source() {
                return ResponseEntity.ok(
                                ApiResponseModel.<List<DataSource>>builder()
                                                .message("Get data ingestion sources successfully")
                                                .data(Arrays.asList(DataSource.values()))
                                                .build());
        }

        @Operation(summary = "Get list of available data ingestion statuses", description = "Get list of available data ingestion statuses")
        @GetMapping("/ingestion_status")
        ResponseEntity<ApiResponseModel<List<IngestionStatus>>> ingestionStatus() {
                return ResponseEntity.ok(
                                ApiResponseModel.<List<IngestionStatus>>builder()
                                                .message("Get data ingestion statuses successfully")
                                                .data(Arrays.asList(IngestionStatus.values()))
                                                .build());
        }      

        @Operation(summary = "Get details info by id (folder or file)", description = "Get detail of a single data ingestion item")
        @GetMapping("/{dataIngestionId}")
        ResponseEntity<ApiResponseModel<DataIngestionResponseDto>> get(
                @Parameter(description = "Data ingestion ID", example = "1e8f9c7d-4b2a-4c3b-8d9e-1f2a3b4c5d6e") @PathVariable UUID dataIngestionId
        ) {
                DataIngestionEntity dataIngestion = dataIngestionService.getEntityById(dataIngestionId);
                if(!permissionCheckerService.canAccess(dataIngestion.getOrganization().getId(), "DATASET_" + dataIngestion.getAccessLevel().name(), "READ", null)){
                        throw new RuntimeException("You don't have permission to access this data ingestion");
                }
                return ResponseEntity.ok(
                                ApiResponseModel.<DataIngestionResponseDto>builder()
                                                .message("Get data ingestion successfully")
                                                .data(dataIngestionService.getById(dataIngestionId))
                                                .build());
        }

        @Operation(summary = "Download data ingestion file", description = "Download file bytes from MinIO by data ingestion id")
        @GetMapping("/{dataIngestionId}/download")
        ResponseEntity<byte[]> download(
                @Parameter(description = "Data ingestion ID", example = "1e8f9c7d-4b2a-4c3b-8d9e-1f2a3b4c5d6e") @PathVariable UUID dataIngestionId
        ) {
                DataIngestionEntity dataIngestion = dataIngestionService.getEntityById(dataIngestionId);
                if(!permissionCheckerService.canAccess(dataIngestion.getOrganization().getId(), "DATASET_" + dataIngestion.getAccessLevel().name(), "READ", null)){
                        throw new RuntimeException("You don't have permission to access this data ingestion");
                }

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
                        @Parameter(description = "Data ingestion ID", example = "1e6633fb-2654-4bd5-aa7d-51bb86418987") @PathVariable UUID dataIngestionId,
                        @Parameter(description = "URL expiration in seconds, default 900", example = "900") @RequestParam(required = false) Integer expiresInSeconds) {
                DataIngestionEntity dataIngestion = dataIngestionService.getEntityById(dataIngestionId);
                if(!permissionCheckerService.canAccess(dataIngestion.getOrganization().getId(), "DATASET_" + dataIngestion.getAccessLevel().name(), "READ", null)){
                        throw new RuntimeException("You don't have permission to access this data ingestion");
                }

                return ResponseEntity.ok(
                                ApiResponseModel.<DataIngestionPresignedUrlResponseDto>builder()
                                                .message("Get data ingestion download url successfully")
                                                .data(dataIngestionService.getPresignedDownloadUrl(dataIngestionId,
                                                                expiresInSeconds))
                                                .build());
        }

        @Operation(summary = "Upload data ingestion file", description = "Upload a data ingestion file to MinIO and optionally trigger ingestion")
        @PreAuthorize("@perm.canAccess(#requestDto.organizationId, 'DATASET_' + #requestDto.accessLevel, 'CREATE',null)")
        @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        ResponseEntity<ApiResponseModel<DataIngestionResponseDto>> upload(
                        @Valid @ModelAttribute DataIngestionUploadRequestDto requestDto) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DataIngestionResponseDto>builder()
                                                .message("Upload data ingestion successfully")
                                                .data(dataIngestionService.uploadDataIngestion(requestDto, DataSource.DOCUMENT))
                                                .build());
        }

        @Operation(summary = "Create data ingestion folder", description = "Create a folder node in the data ingestion tree without file upload")
        @PreAuthorize("@perm.canAccess(#requestDto.organizationId, 'DATASET_' + #requestDto.accessLevel, 'CREATE',null)")
        @PostMapping("/folders")
        ResponseEntity<ApiResponseModel<DataIngestionResponseDto>> createFolder(
                        @Valid @RequestBody DataIngestionCreateFolderRequestDto requestDto) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DataIngestionResponseDto>builder()
                                                .message("Create data ingestion folder successfully")
                                                .data(dataIngestionService.createFolder(requestDto, DataSource.DOCUMENT))
                                                .build());
        }

        @Operation(summary = "Get data ingestion list (folders and files)", description = "Get paginated list of data ingestion items with optional filters. Use formSources to filter by multiple sources (e.g. formSources=SYSTEM&formSources=DOCUMENT)")
        @PreAuthorize("@perm.canAccess(#filterDto.organizationId, 'DATASET_' + #filterDto.accessLevel.name(), 'READ',null)")
        @GetMapping("")
        ResponseEntity<ApiResponseModel<List<DataIngestionResponseDto>>> list(
                        @ModelAttribute @Valid DataIngestionFilterDto filterDto) {
                List<PermissionGrantModel> permissions = ourService.getPermissionGrant(JwtUtil.getUserId(), JwtUtil.getOrgId());
                switch (filterDto.getAccessLevel()){
                        case PERSONAL -> {
                                // Nếu scope là OWN thì sẽ filter theo owner
                                filterDto.setOwnerId(JwtUtil.getUserId());
                        }
                        case LOCAL -> {
                                boolean hasAllScope = false;
                                boolean hasDescendantScope = false;
                                // Nếu scope là ALL thì sẽ lấy tất cả data ingestion của org, không cần filter theo owner
                                for (PermissionGrantModel p : permissions) {
                                        if (p.getResource().equals(PermissionResource.DATASET_LOCAL) && p.getAction().equals(PermissionAction.READ) && p.getScope().equals(PermissionScope.ALL)) {
                                                filterDto.setOwnerId(null);
                                                hasAllScope = true;
                                                break;
                                        }
                                }

                                // Nếu scope là DESCENDANT thì sẽ lấy tất cả data ingestion của org và descendant org, không cần filter theo owner
                                if(organizationService.isDescendant(JwtUtil.getOrgId(), filterDto.getOrganizationId())) {
                                        filterDto.setOwnerId(null);
                                        hasDescendantScope = true;
                                        break;
                                }

                                // Nếu scope là OWN thì sẽ filter theo owner
                                if(!hasAllScope && !hasDescendantScope && filterDto.getOwnerId() == null)
                                        filterDto.setOwnerId(JwtUtil.getUserId());
                        }
                        case GLOBAL -> {
                                boolean hasAllScope = false;
                                boolean hasDescendantScope = false;
                                // Nếu scope là ALL thì sẽ lấy tất cả data ingestion của org, không cần filter theo owner
                                for (PermissionGrantModel p : permissions) {
                                        if (p.getResource().equals(PermissionResource.DATASET_GLOBAL) && p.getAction().equals(PermissionAction.READ) && p.getScope().equals(PermissionScope.ALL)) {
                                                filterDto.setOwnerId(null);
                                                hasAllScope = true;
                                                break;
                                        }
                                }

                                // Nếu scope là DESCENDANT thì sẽ lấy tất cả data ingestion của org và descendant org, không cần filter theo owner
                                if(organizationService.isDescendant(JwtUtil.getOrgId(), filterDto.getOrganizationId())) {
                                        filterDto.setOwnerId(null);
                                        hasDescendantScope = true;
                                        break;
                                }

                                // Nếu scope là OWN thì sẽ filter theo owner
                                if(!hasAllScope && !hasDescendantScope && filterDto.getOwnerId() == null)
                                        filterDto.setOwnerId(JwtUtil.getUserId());
                        } 
                }                

                // Lấy formSource là SYSTEM và DOCUMENT
                filterDto.setFormSources(Arrays.asList(DataSource.SYSTEM, DataSource.DOCUMENT));

                Page<DataIngestionResponseDto> page = dataIngestionService.getAll(filterDto);
                return ResponseEntity.ok(
                                ApiResponseModel.<List<DataIngestionResponseDto>>builder()
                                                .message("Get data ingestion list successfully")
                                                .count(page.getTotalElements())
                                                .data(page.getContent())
                                                .build());
        }
        

        @Operation(summary = "Rename or move folder", description = "Update folder name and/or move folder to another parent")
        @PreAuthorize("@perm.canAccess(#requestDto.organizationId, 'DATASET_' + #requestDto.accessLevel, 'CREATE',null)")
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

        @Operation(summary = "Receive ingestion callback status", description = "Webhook endpoint để ingestion service callback trạng thái job")
        @PostMapping(value = "/ingestion/webhook/status", consumes = MediaType.APPLICATION_JSON_VALUE)
        ResponseEntity<ApiResponseModel<DataIngestionJobStatusResponseDto>> ingestionWebhookStatus(
                        @RequestBody IngestionStatusResponseDto callbackPayload) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DataIngestionJobStatusResponseDto>builder()
                                                .message("Receive ingestion callback successfully")
                                                .data(dataIngestionService.handleIngestionCallback(callbackPayload))
                                                .build());
        }

        @Operation(summary = "Delete data ingestion", description = "Delete a single data ingestion item by its ID")
        @PreAuthorize("@perm.canAccess(#organizationId, 'DATASET_' + #accessLevel, 'DELETE',null)")
        @DeleteMapping("/{dataIngestionId}")
        ResponseEntity<ApiResponseModel<DataIngestionResponseDto>> delete(@PathVariable UUID dataIngestionId,
                @RequestParam(required = true) UUID organizationId,
                @RequestParam(required = true) String accessLevel
        ) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DataIngestionResponseDto>builder()
                                                .message("Delete data ingestion successfully")
                                                .data(dataIngestionService.deleteById(dataIngestionId))
                                                .build());
        }

        @Operation(summary = "Delete folder", description = "Delete a single folder by its ID, and all its descendant data ingestion items will be deleted as well")
        @PreAuthorize("@perm.canAccess(#organizationId, 'DATASET_' + #accessLevel, 'DELETE',null)")
        @DeleteMapping("/folders/{dataIngestionId}")
        ResponseEntity<ApiResponseModel<Void>> deleteFolder(@PathVariable UUID dataIngestionId,
                @RequestParam(required = true) UUID organizationId,
                @RequestParam(required = true) String accessLevel
        ) {
                dataIngestionService.deleteFolderById(dataIngestionId);
                return ResponseEntity.ok(
                                ApiResponseModel.<Void>builder()
                                                .message("Delete data ingestion folder successfully")
                                                .build());
        }

        @GetMapping("/organizations/{organizationId}/can-access-list")
        @PreAuthorize("@perm.canAccess(#organizationId, 'DATASET_' + #accessLevel, 'READ',null)")
        ResponseEntity<ApiResponseModel<List<OrganizationResponseDto>>> getOrganizationsCanAccess(
                @PathVariable UUID organizationId, 
                @Valid @Min(value = 0, message = InputValidateKey.NESTED_CHILD_VALUE_INVALID) @RequestParam(required = false) Integer nestedChild,
                @RequestParam(required = true) String accessLevel){

                List<PermissionGrantModel> permissions = ourService.getPermissionGrant(JwtUtil.getUserId(), JwtUtil.getOrgId());
                boolean hasAllScope = false;
                boolean hasDescendantScope = false;

                switch (accessLevel){
                        case "PERSONAL" -> {

                        }
                        case "LOCAL" -> {
                                // Nếu scope là ALL thì sẽ lấy tất cả data ingestion của org, không cần filter theo owner
                                for (PermissionGrantModel p : permissions) {
                                        if (p.getResource().equals(PermissionResource.DATASET_LOCAL) && p.getAction().equals(PermissionAction.READ) && p.getScope().equals(PermissionScope.ALL)) {
                                                hasAllScope = true;
                                                break;
                                        }
                                }

                                // Nếu scope là DESCENDANT thì sẽ lấy tất cả data ingestion của org và descendant org, không cần filter theo owner
                                if(organizationService.isDescendant(JwtUtil.getOrgId(), organizationId)) {
                                        hasDescendantScope = true;
                                        break;
                                }
                        }
                        case "GLOBAL" -> {
                                // Nếu scope là ALL thì sẽ lấy tất cả data ingestion của org, không cần filter theo owner
                                for (PermissionGrantModel p : permissions) {
                                        if (p.getResource().equals(PermissionResource.DATASET_GLOBAL) && p.getAction().equals(PermissionAction.READ) && p.getScope().equals(PermissionScope.ALL)) {
                                                hasAllScope = true;
                                                break;
                                        }
                                }

                                // Nếu scope là DESCENDANT thì sẽ lấy tất cả data ingestion của org và descendant org, không cần filter theo owner
                                if(organizationService.isDescendant(JwtUtil.getOrgId(), organizationId)) {
                                        hasDescendantScope = true;
                                        break;
                                }
                        }
                }

                if(hasAllScope) {
                        OrganizationResponseDto rootOrg = organizationService.getRoot(nestedChild);
                        return ResponseEntity.ok(
                                ApiResponseModel.<List<OrganizationResponseDto>>builder()
                                                .message("Get list organizations can access successfully")
                                                .data(List.of(rootOrg))
                                                .build());
                } else if (hasDescendantScope) {
                        CustomPairModel<Long, List<OrganizationResponseDto>> result = organizationService.getChild(organizationId, nestedChild, new OrganizationFilterDto());
                        return ResponseEntity.ok(
                                ApiResponseModel.<List<OrganizationResponseDto>>builder()
                                                .message("Get list organizations can access successfully")
                                                .data(result.getSecond())
                                                .build());
                } else {
                        return ResponseEntity.ok(
                                ApiResponseModel.<List<OrganizationResponseDto>>builder()
                                                .message("Get list organizations can access successfully")
                                                .data(organizationService.getById(organizationId, nestedChild) != null ? List.of(organizationService.getById(organizationId, nestedChild)) : List.of())
                                                .build());
                }
        }

}
