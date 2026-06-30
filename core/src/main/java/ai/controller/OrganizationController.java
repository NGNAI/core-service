package ai.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.constant.InputValidateKey;
import ai.dto.own.request.filter.OrganizationFilterDto;
import ai.dto.own.response.OrganizationResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/organizations")
@Tag(name = "Organization", description = "Organization APIs")
@RestController
public class OrganizationController {
    OrganizationService organizationService;

        @Operation(summary = "Get organization by ID", description = "Retrieve organization details by its UUID")
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
    
    @Operation(summary = "Get organizations by permission", description = "Retrieve list of organizations the current user has permission to access")
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

    @Operation(summary = "Get root organization", description = "Retrieve the root organization list")
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

    @Operation(summary = "Get children organizations", description = "Retrieve child organizations of a given organization")
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

   
}
