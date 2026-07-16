package ai.controller.admin;

import java.util.List;
import java.util.UUID;

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
import ai.dto.own.request.OrganizationCreateRequestDto;
import ai.dto.own.request.OrganizationUpdateRequestDto;
import ai.dto.own.request.filter.OrganizationFilterDto;
import ai.dto.own.response.OrganizationResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.OrganizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/organizations")
@Tag(name = "Organization Admin", description = "Organization admin APIs")
@RestController
public class OrganizationAdminController {
    OrganizationService organizationService;

        @Operation(summary = "Get organization by ID", description = "Retrieve an organization by its ID with optional nested child depth")
        @GetMapping("/{organizationId}")
        @PreAuthorize("@perm.canAccess(#organizationId, 'ORG', 'READ',null)")
        ResponseEntity<ApiResponseModel<OrganizationResponseDto>> getById(@PathVariable UUID organizationId
            , @Valid @Min(value = 0, message = InputValidateKey.NESTED_CHILD_VALUE_INVALID) @RequestParam(required = false) Integer nestedChild){
        return ResponseEntity.ok(
                ApiResponseModel.<OrganizationResponseDto>builder()
                        .message("Get organization successfully")
                        .data(organizationService.getById(organizationId,nestedChild))
                        .build()
        );
    }

    @Operation(summary = "Get all organizations", description = "Retrieve a paginated list of organizations based on filter criteria")
    @GetMapping
    @PreAuthorize("@perm.canAccess(null, 'ORG', 'READ',null)")
    ResponseEntity<ApiResponseModel<List<OrganizationResponseDto>>> getAll(@Valid @ModelAttribute OrganizationFilterDto filterDto){
        CustomPairModel<Long, List<OrganizationResponseDto>> result = organizationService.getAll(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<OrganizationResponseDto>>builder()
                        .message("Get list organizations successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @Operation(summary = "Get organizations by permission", description = "Retrieve organizations that the current user has permission to view")
    @GetMapping("/on-permission")
    ResponseEntity<ApiResponseModel<List<OrganizationResponseDto>>> getByPermission(@Valid @ModelAttribute OrganizationFilterDto filterDto){
        CustomPairModel<Long, List<OrganizationResponseDto>> result = organizationService.getByPermission(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<OrganizationResponseDto>>builder()
                        .message("Get list organizations successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @Operation(summary = "Get root organization", description = "Retrieve the root organization with optional nested child depth")
    @GetMapping("/root")
    @PreAuthorize("@perm.canAccess(null, 'ORG', 'READ',null)")
    ResponseEntity<ApiResponseModel<OrganizationResponseDto>> getRoot(@Valid @Min(value = 0, message = InputValidateKey.NESTED_CHILD_VALUE_INVALID) @RequestParam(required = false) Integer nestedChild){
        return ResponseEntity.ok(
                ApiResponseModel.<OrganizationResponseDto>builder()
                        .message("Get root list organizations successfully")
                        .data(organizationService.getRoot(nestedChild))
                        .build()
        );
    }

    @Operation(summary = "Get organization children", description = "Retrieve child organizations of a parent organization with pagination and filtering")
    @GetMapping("/{organizationId}/children")
    @PreAuthorize("@perm.canAccess(#organizationId, 'ORG', 'READ',null)")
    ResponseEntity<ApiResponseModel<List<OrganizationResponseDto>>> getChild(@PathVariable UUID organizationId, @Valid @Min(value = 0, message = InputValidateKey.NESTED_CHILD_VALUE_INVALID) @RequestParam(required = false) Integer nestedChild,@Valid @ModelAttribute OrganizationFilterDto filterDto){
        CustomPairModel<Long, List<OrganizationResponseDto>> result = organizationService.getChild(organizationId, nestedChild, filterDto);

        return ResponseEntity.ok(
                ApiResponseModel.<List<OrganizationResponseDto>>builder()
                        .message("Get children of organizations successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @Operation(summary = "Create organization", description = "Create a new organization under a specified parent organization")
    @PostMapping
    @PreAuthorize("@perm.canAccess(#requestDto.parentId, 'ORG', 'CREATE',null)")
    ResponseEntity<ApiResponseModel<OrganizationResponseDto>> create(@Valid @RequestBody OrganizationCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<OrganizationResponseDto>builder()
                        .message("Create organization successfully")
                        .data(organizationService.create(requestDto))
                        .build()
        );
    }

    @Operation(summary = "Update organization", description = "Update an existing organization by ID")
    @PutMapping("/{organizationId}")
    @PreAuthorize("@perm.canAccess(#organizationId, 'ORG', 'UPDATE',null)")
    ResponseEntity<ApiResponseModel<OrganizationResponseDto>> update(@PathVariable UUID organizationId,@Valid @RequestBody OrganizationUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<OrganizationResponseDto>builder()
                        .message("Update organization successfully")
                        .data(organizationService.update(organizationId, requestDto))
                        .build()
        );
    }

    @Operation(summary = "Delete organization", description = "Delete an organization by its ID")
    @DeleteMapping("/{organizationId}")
    @PreAuthorize("@perm.canAccess(#organizationId, 'ORG', 'DELETE',null)")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID organizationId){
        organizationService.delete(organizationId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete organization successfully")
                        .build()
        );
    }
}
