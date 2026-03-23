package ai.controller;

import ai.dto.own.request.*;
import ai.dto.own.request.filter.OrganizationFilterDto;
import ai.dto.own.request.filter.UserFilterDto;
import ai.dto.own.response.OrganizationResponseDto;
import ai.dto.own.response.UserResponseDto;
import ai.dto.own.response.UserWithRoleInOrgResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.OrganizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/organizations/{organizationId}/users")
@RestController
public class OrganizationUserRoleController {
    OrganizationService organizationService;

    ResponseEntity<ApiResponseModel<List<UserWithRoleInOrgResponseDto>>> getUserByOrgId(@PathVariable int organizationId, @ModelAttribute UserFilterDto userFilterDto){
        CustomPairModel<Long, List<UserWithRoleInOrgResponseDto>> result = organizationService.getUsersByOrgId(organizationId, userFilterDto);

        return ResponseEntity.ok(
                ApiResponseModel.<List<UserWithRoleInOrgResponseDto>>builder()
                        .message("Get users in organization successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @GetMapping("/unassigned")
    ResponseEntity<ApiResponseModel<List<UserResponseDto>>> getUserNotInOrg(@PathVariable int organizationId, @ModelAttribute UserFilterDto userFilterDto){
        CustomPairModel<Long, List<UserResponseDto>> result = organizationService.getUsersNotInOrg(organizationId,userFilterDto);

        
        return ResponseEntity.ok(
                ApiResponseModel.<List<UserResponseDto>>builder()
                        .message("Get users not in organization successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @PostMapping()
    ResponseEntity<ApiResponseModel<Void>> assignUsers(@PathVariable int organizationId, @Valid @RequestBody OrganizationAssignUserRequestDto requestDto){
        organizationService.assignUsers(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Assign users into organization successfully")
                        .build()
        );
    }

    @PostMapping("/remove")
    ResponseEntity<ApiResponseModel<Void>> removeUsers(@PathVariable int organizationId,@Valid @RequestBody OrganizationRemoveUserRequestDto requestDto){
        organizationService.removeUsers(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Remove users from organization successfully")
                        .build()
        );
    }

    @PostMapping("roles/assign")
    ResponseEntity<ApiResponseModel<Void>> assignRole(@PathVariable int organizationId,@Valid @RequestBody OrganizationAssignRoleRequestDto requestDto){
        organizationService.assignRole(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Assign users to role from organization successfully")
                        .build()
        );
    }

    @PostMapping("/roles/remove")
    ResponseEntity<ApiResponseModel<Void>> removeRole(@PathVariable int organizationId,@Valid @RequestBody OrganizationRemoveRoleRequestDto requestDto){
        organizationService.removeRole(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Remove users from role from organization successfully")
                        .build()
        );
    }

    @PostMapping("/roles/replace")
    ResponseEntity<ApiResponseModel<Void>> replaceRole(@PathVariable int organizationId,@Valid @RequestBody OrganizationReplaceRoleRequestDto requestDto){
        organizationService.replaceRole(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Replace users role in organization successfully")
                        .build()
        );
    }

    @PostMapping("/roles/reset")
    ResponseEntity<ApiResponseModel<Void>> resetRole(@PathVariable int organizationId,@Valid @RequestBody OrganizationResetRoleRequestDto requestDto){
        organizationService.resetRole(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Reset users role in organization successfully")
                        .build()
        );
    }
}
