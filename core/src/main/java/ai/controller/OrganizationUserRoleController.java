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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/organizations/{organizationId}/users")
@RestController
public class OrganizationUserRoleController {
    OrganizationUserRoleService ourService;

    @GetMapping()
    ResponseEntity<ApiResponseModel<List<UserWithRoleInOrgResponseDto>>> getUserByOrgId(@PathVariable UUID organizationId, @ModelAttribute UserFilterDto userFilterDto){
        CustomPairModel<Long, List<UserWithRoleInOrgResponseDto>> result = ourService.getUsersByOrgId(organizationId, userFilterDto);

        return ResponseEntity.ok(
                ApiResponseModel.<List<UserWithRoleInOrgResponseDto>>builder()
                        .message("Get users in organization successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @GetMapping("/unassigned")
    ResponseEntity<ApiResponseModel<List<UserResponseDto>>> getUserNotInOrg(@PathVariable UUID organizationId, @ModelAttribute UserFilterDto userFilterDto){
        CustomPairModel<Long, List<UserResponseDto>> result = ourService.getUsersNotInOrg(organizationId,userFilterDto);

        
        return ResponseEntity.ok(
                ApiResponseModel.<List<UserResponseDto>>builder()
                        .message("Get users not in organization successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @PostMapping()
    ResponseEntity<ApiResponseModel<Void>> assignUsers(@PathVariable UUID organizationId, @Valid @RequestBody OrganizationAssignUserRequestDto requestDto){
        ourService.assignUsers(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Assign users into organization successfully")
                        .build()
        );
    }

    @PostMapping("/remove")
    ResponseEntity<ApiResponseModel<Void>> removeUsers(@PathVariable UUID organizationId,@Valid @RequestBody OrganizationRemoveUserRequestDto requestDto){
        ourService.removeUsers(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Remove users from organization successfully")
                        .build()
        );
    }

    @PostMapping("roles/assign")
    ResponseEntity<ApiResponseModel<Void>> assignRole(@PathVariable UUID organizationId,@Valid @RequestBody OrganizationAssignRoleRequestDto requestDto){
        ourService.assignRole(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Assign users to role from organization successfully")
                        .build()
        );
    }

    @PostMapping("/roles/remove")
    ResponseEntity<ApiResponseModel<Void>> removeRole(@PathVariable UUID organizationId,@Valid @RequestBody OrganizationRemoveRoleRequestDto requestDto){
        ourService.removeRole(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Remove users from role from organization successfully")
                        .build()
        );
    }

    @PostMapping("/roles/replace")
    ResponseEntity<ApiResponseModel<Void>> replaceRole(@PathVariable UUID organizationId,@Valid @RequestBody OrganizationReplaceRoleRequestDto requestDto){
        ourService.replaceRole(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Replace users role in organization successfully")
                        .build()
        );
    }

    @PostMapping("/roles/reset")
    ResponseEntity<ApiResponseModel<Void>> resetRole(@PathVariable UUID organizationId,@Valid @RequestBody OrganizationResetRoleRequestDto requestDto){
        ourService.resetRole(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Reset users role in organization successfully")
                        .build()
        );
    }
}
