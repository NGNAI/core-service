package ai.controller.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.request.OrganizationAssignRoleRequestDto;
import ai.dto.own.request.OrganizationAssignUserRequestDto;
import ai.dto.own.request.OrganizationRemoveRoleRequestDto;
import ai.dto.own.request.OrganizationRemoveUserRequestDto;
import ai.dto.own.request.OrganizationReplaceRoleRequestDto;
import ai.dto.own.request.OrganizationResetRoleRequestDto;
import ai.dto.own.request.filter.UserFilterDto;
import ai.dto.own.response.UserResponseDto;
import ai.dto.own.response.UserWithRoleInOrgResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.OrganizationUserRoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/organizations/{organizationId}")
@Tag(name = "Organization User Role Admin", description = "Organization user role admin APIs")
@RestController
public class OrganizationUserRoleAdminController {
    OrganizationUserRoleService ourService;

    @Operation(summary = "Get users in organization", description = "Retrieve users with roles in the specified organization, supporting pagination and filtering")
    @GetMapping("/users")
    @PreAuthorize("@perm.canAccess(#organizationId, 'ORG', 'READ',null)")
    ResponseEntity<ApiResponseModel<List<UserWithRoleInOrgResponseDto>>> getUserByOrgId(@PathVariable UUID organizationId,@Valid @ModelAttribute UserFilterDto userFilterDto){
        CustomPairModel<Long, List<UserWithRoleInOrgResponseDto>> result = ourService.getUsersByOrgId(organizationId, userFilterDto);

        return ResponseEntity.ok(
                ApiResponseModel.<List<UserWithRoleInOrgResponseDto>>builder()
                        .message("Get users in organization successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @Operation(summary = "Get users not in organization", description = "Retrieve users not assigned to the organization, supporting pagination and filtering")
    @GetMapping("/users/unassigned")
    @PreAuthorize("@perm.canAccess(#organizationId, 'ORG', 'READ',null)")
    ResponseEntity<ApiResponseModel<List<UserResponseDto>>> getUserNotInOrg(@PathVariable UUID organizationId,@Valid @ModelAttribute UserFilterDto userFilterDto){
        CustomPairModel<Long, List<UserResponseDto>> result = ourService.getUsersNotInOrg(organizationId,userFilterDto);

        
        return ResponseEntity.ok(
                ApiResponseModel.<List<UserResponseDto>>builder()
                        .message("Get users not in organization successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @Operation(summary = "Get users with specific role", description = "Retrieve users who have a specific role within the organization, supporting pagination and filtering")
    @GetMapping("/roles/{roleId}/users")
    @PreAuthorize("@perm.canAccess(#organizationId, 'ORG', 'READ',null)")
    ResponseEntity<ApiResponseModel<List<UserWithRoleInOrgResponseDto>>> getUserHasRoleInOrg(@PathVariable UUID organizationId, @PathVariable UUID roleId,@Valid @ModelAttribute UserFilterDto userFilterDto){
        CustomPairModel<Long, List<UserWithRoleInOrgResponseDto>> result = ourService.getUsersByOrgIdAndInRole(organizationId, roleId, userFilterDto);

        return ResponseEntity.ok(
                ApiResponseModel.<List<UserWithRoleInOrgResponseDto>>builder()
                        .message("Get users have role in organization successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @Operation(summary = "Get users without specific role", description = "Retrieve users who do not have a specific role within the organization, supporting pagination and filtering")
    @GetMapping("/roles/{roleId}/users/unassigned")
    @PreAuthorize("@perm.canAccess(#organizationId, 'ORG', 'READ',null)")
    ResponseEntity<ApiResponseModel<List<UserWithRoleInOrgResponseDto>>> getUserNotHasRoleInOrg(@PathVariable UUID organizationId, @PathVariable UUID roleId,@Valid @ModelAttribute UserFilterDto userFilterDto){
        CustomPairModel<Long, List<UserWithRoleInOrgResponseDto>> result = ourService.getUsersByOrgIdAndNotInRole(organizationId, roleId, userFilterDto);

        return ResponseEntity.ok(
                ApiResponseModel.<List<UserWithRoleInOrgResponseDto>>builder()
                        .message("Get users don't have role in organization successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @Operation(summary = "Assign users to organization", description = "Assign a list of users to the organization")
    @PostMapping("/users")
    @PreAuthorize("@perm.canAccess(#organizationId, 'ORG', 'ASSIGN', 'USER')")
    ResponseEntity<ApiResponseModel<Void>> assignUsers(@PathVariable UUID organizationId, @Valid @RequestBody OrganizationAssignUserRequestDto requestDto){
        ourService.assignUsers(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Assign users into organization successfully")
                        .build()
        );
    }

    @Operation(summary = "Remove users from organization", description = "Remove a list of users from the organization")
    @PostMapping("/users/remove")
    @PreAuthorize("@perm.canAccess(#organizationId, 'ORG', 'REMOVE', 'USER')")
    ResponseEntity<ApiResponseModel<Void>> removeUsers(@PathVariable UUID organizationId,@Valid @RequestBody OrganizationRemoveUserRequestDto requestDto){
        ourService.removeUsers(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Remove users from organization successfully")
                        .build()
        );
    }

    @Operation(summary = "Assign role to users", description = "Assign a role to users within the organization")
    @PostMapping("/roles/{roleId}/users")
    @PreAuthorize("@perm.canAccess(#organizationId, 'ORG', 'ASSIGN', 'ROLE')")
    ResponseEntity<ApiResponseModel<Void>> assignRole(@PathVariable UUID organizationId, @PathVariable UUID roleId,@Valid @RequestBody OrganizationAssignRoleRequestDto requestDto){
        ourService.assignRole(organizationId, roleId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Assign users to role from organization successfully")
                        .build()
        );
    }

    @Operation(summary = "Remove role from users", description = "Remove a role from users within the organization")
    @PostMapping("/roles/{roleId}/users/remove")
    @PreAuthorize("@perm.canAccess(#organizationId, 'ORG', 'REMOVE', 'ROLE')")
    ResponseEntity<ApiResponseModel<Void>> removeRole(@PathVariable UUID organizationId, @PathVariable UUID roleId,@Valid @RequestBody OrganizationRemoveRoleRequestDto requestDto){
        ourService.removeRole(organizationId, roleId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Remove users from role from organization successfully")
                        .build()
        );
    }

    @Operation(summary = "Replace user role", description = "Replace users' roles within the organization")
    @PostMapping("/users/roles/replace")
    @PreAuthorize("@perm.canAccess(#organizationId, 'ORG', 'ASSIGN', 'ROLE')")
    ResponseEntity<ApiResponseModel<Void>> replaceRole(@PathVariable UUID organizationId,@Valid @RequestBody OrganizationReplaceRoleRequestDto requestDto){
        ourService.replaceRole(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Replace users role in organization successfully")
                        .build()
        );
    }

    @Operation(summary = "Reset user role", description = "Reset all users' roles within the organization to default")
    @PostMapping("/users/roles/reset")
    @PreAuthorize("@perm.canAccess(#organizationId, 'ORG', 'ASSIGN', 'ROLE')")
    ResponseEntity<ApiResponseModel<Void>> resetRole(@PathVariable UUID organizationId,@Valid @RequestBody OrganizationResetRoleRequestDto requestDto){
        ourService.resetRole(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Reset users role in organization successfully")
                        .build()
        );
    }
}
