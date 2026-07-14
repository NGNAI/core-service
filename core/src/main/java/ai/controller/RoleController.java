package ai.controller;

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
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.request.RoleCreateRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ai.dto.own.request.RolePermissionUpdateRequestDto;
import ai.dto.own.request.RoleUpdateRequestDto;
import ai.dto.own.request.filter.RoleFilterDto;
import ai.dto.own.response.RoleResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.RoleService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/roles")
@Tag(name = "Role Admin", description = "Role admin APIs")
@RestController
public class RoleController {
    RoleService roleService;

    @Operation(summary = "Get all roles", description = "Retrieve a paginated list of roles based on filter criteria")
    @GetMapping
    @PreAuthorize("@perm.canAccess(null, 'ROLE', 'READ',null)")
    ResponseEntity<ApiResponseModel<List<RoleResponseDto>>> getAll(@Valid @ModelAttribute RoleFilterDto filterDto){
        CustomPairModel<Long, List<RoleResponseDto>> result = roleService.getAll(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<RoleResponseDto>>builder()
                        .message("Get list roles successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @Operation(summary = "Get role by ID", description = "Retrieve details of a specific role by its UUID")
    @GetMapping("/{roleId}")
    @PreAuthorize("@perm.canAccess(null, 'ROLE', 'READ',null)")
    ResponseEntity<ApiResponseModel<RoleResponseDto>> getById(@PathVariable UUID roleId){
        return ResponseEntity.ok(
                ApiResponseModel.<RoleResponseDto>builder()
                        .message("Get role successfully")
                        .data(roleService.getById(roleId))
                        .build()
        );
    }

    @Operation(summary = "Create role", description = "Create a new role with the provided details")
    @PostMapping
    @PreAuthorize("@perm.canAccess(null, 'ROLE', 'CREATE', null)")
    ResponseEntity<ApiResponseModel<RoleResponseDto>> create(@Valid @RequestBody RoleCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<RoleResponseDto>builder()
                        .message("Create roles successfully")
                        .data(roleService.create(requestDto))
                        .build()
        );
    }

    @Operation(summary = "Update role", description = "Update an existing role identified by roleId")
    @PutMapping("/{roleId}")
    @PreAuthorize("@perm.canAccess(null, 'ROLE', 'UPDATE', null)")
    ResponseEntity<ApiResponseModel<RoleResponseDto>> update(@PathVariable UUID roleId,@Valid  @RequestBody RoleUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<RoleResponseDto>builder()
                        .message("Update role successfully")
                        .data(roleService.update(roleId, requestDto))
                        .build()
        );
    }

    @Operation(summary = "Assign permissions to role", description = "Assign a set of permissions to the specified role")
    @PostMapping("/{roleId}/permissions")
    @PreAuthorize("@perm.canAccess(null, 'ROLE', 'ASSIGN', 'PERMISSION')")
    ResponseEntity<ApiResponseModel<RoleResponseDto>> assignPermission(@PathVariable UUID roleId,@Valid @RequestBody RolePermissionUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<RoleResponseDto>builder()
                        .message("Assign permissions to successfully")
                        .data(roleService.assignPermissions(roleId, requestDto))
                        .build()
        );
    }

    @Operation(summary = "Delete role", description = "Delete a role by its UUID")
    @DeleteMapping("/{roleId}")
    @PreAuthorize("@perm.canAccess(null, 'ROLE', 'DELETE', null)")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID roleId){
        roleService.delete(roleId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete role successfully")
                        .build()
        );
    }
}
