package ai.controller;

import ai.dto.own.request.*;
import ai.dto.own.response.OrganizationResponseDto;
import ai.dto.own.response.UserResponseDto;
import ai.dto.own.response.UserWithRoleInOrgResponseDto;
import ai.model.ApiResponseModel;
import ai.service.OrganizationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/prv/organization")
@RestController
public class OrganizationController {
    OrganizationService organizationService;

    @GetMapping("/{organizationId}")
    ResponseEntity<ApiResponseModel<OrganizationResponseDto>> getById(@PathVariable int organizationId, @RequestParam(required = false) Integer nestedChild){
        return ResponseEntity.ok(
                ApiResponseModel.<OrganizationResponseDto>builder()
                        .message("Get organization successfully")
                        .data(organizationService.getById(organizationId,nestedChild))
                        .build()
        );
    }

    @GetMapping
    ResponseEntity<ApiResponseModel<List<OrganizationResponseDto>>> getAll(){
        return ResponseEntity.ok(
                ApiResponseModel.<List<OrganizationResponseDto>>builder()
                        .message("Get list organizations successfully")
                        .data(organizationService.getAll())
                        .build()
        );
    }

    @GetMapping("/getRoot")
    ResponseEntity<ApiResponseModel<List<OrganizationResponseDto>>> getRoot(@RequestParam(required = false) Integer nestedChild){
        return ResponseEntity.ok(
                ApiResponseModel.<List<OrganizationResponseDto>>builder()
                        .message("Get root list organizations successfully")
                        .data(organizationService.getRoot(nestedChild))
                        .build()
        );
    }

    @GetMapping("/getChild/{organizationId}")
    ResponseEntity<ApiResponseModel<List<OrganizationResponseDto>>> getChild(@PathVariable int organizationId, @RequestParam(required = false) Integer nestedChild){
        return ResponseEntity.ok(
                ApiResponseModel.<List<OrganizationResponseDto>>builder()
                        .message("Get children of organizations successfully")
                        .data(organizationService.getChild(organizationId, nestedChild))
                        .build()
        );
    }

    @GetMapping("/{organizationId}/users")
    ResponseEntity<ApiResponseModel<List<UserWithRoleInOrgResponseDto>>> getUserByOrgId(@PathVariable int organizationId){
        return ResponseEntity.ok(
                ApiResponseModel.<List<UserWithRoleInOrgResponseDto>>builder()
                        .message("Get users in organization successfully")
                        .data(organizationService.getUsersByOrgId(organizationId))
                        .build()
        );
    }

    @GetMapping("/{organizationId}/users/not-assign")
    ResponseEntity<ApiResponseModel<List<UserResponseDto>>> getUserNotInOrg(@PathVariable int organizationId){
        return ResponseEntity.ok(
                ApiResponseModel.<List<UserResponseDto>>builder()
                        .message("Get users not in organization successfully")
                        .data(organizationService.getUsersNotInOrg(organizationId))
                        .build()
        );
    }

    @PostMapping
    ResponseEntity<ApiResponseModel<OrganizationResponseDto>> create(@RequestBody OrganizationCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<OrganizationResponseDto>builder()
                        .message("Create organization successfully")
                        .data(organizationService.create(requestDto))
                        .build()
        );
    }

    @PostMapping("/{organizationId}/users")
    ResponseEntity<ApiResponseModel<Void>> assignUsers(@PathVariable int organizationId, @RequestBody OrganizationAssignUserRequestDto requestDto){
        organizationService.assignUsers(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Assign users into organization successfully")
                        .build()
        );
    }

    @PostMapping("/{organizationId}/users/remove")
    ResponseEntity<ApiResponseModel<Void>> removeUsers(@PathVariable int organizationId, @RequestBody OrganizationRemoveUserRequestDto requestDto){
        organizationService.removeUsers(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Remove users from organization successfully")
                        .build()
        );
    }

    @PostMapping("/{organizationId}/users/roles")
    ResponseEntity<ApiResponseModel<Void>> assignRole(@PathVariable int organizationId, @RequestBody OrganizationAssignRoleRequestDto requestDto){
        organizationService.assignRole(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Assign users to role from organization successfully")
                        .build()
        );
    }

    @PostMapping("/{organizationId}/users/roles/remove")
    ResponseEntity<ApiResponseModel<Void>> removeRole(@PathVariable int organizationId, @RequestBody OrganizationRemoveRoleRequestDto requestDto){
        organizationService.removeRole(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Remove users from role from organization successfully")
                        .build()
        );
    }

    @PostMapping("/{organizationId}/users/roles/replace")
    ResponseEntity<ApiResponseModel<Void>> replaceRole(@PathVariable int organizationId, @RequestBody OrganizationReplaceRoleRequestDto requestDto){
        organizationService.replaceRole(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Replace users role in organization successfully")
                        .build()
        );
    }

    @PostMapping("/{organizationId}/users/roles/reset")
    ResponseEntity<ApiResponseModel<Void>> resetRole(@PathVariable int organizationId, @RequestBody OrganizationResetRoleRequestDto requestDto){
        organizationService.resetRole(organizationId, requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Reset users role in organization successfully")
                        .build()
        );
    }

    @PutMapping("/{organizationId}")
    ResponseEntity<ApiResponseModel<OrganizationResponseDto>> update(@PathVariable int organizationId, @RequestBody OrganizationUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<OrganizationResponseDto>builder()
                        .message("Update organization successfully")
                        .data(organizationService.update(organizationId, requestDto))
                        .build()
        );
    }

    @DeleteMapping("/{organizationId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable int organizationId){
        organizationService.delete(organizationId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete organization successfully")
                        .build()
        );
    }
}
