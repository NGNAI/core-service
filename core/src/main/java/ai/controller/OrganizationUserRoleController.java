package ai.controller;

import ai.dto.own.request.*;
import ai.dto.own.request.filter.UserFilterDto;
import ai.dto.own.response.UserResponseDto;
import ai.dto.own.response.UserWithRoleInOrgResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.OrganizationUserRoleService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/organizations/{organizationId}")
@RestController
public class OrganizationUserRoleController {
    OrganizationUserRoleService ourService;

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
