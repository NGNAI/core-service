package ai.controller;

import ai.dto.own.request.RoleCreateRequestDto;
import ai.dto.own.request.RolePermissionUpdateRequestDto;
import ai.dto.own.request.RoleUpdateRequestDto;
import ai.dto.own.request.filter.PermissionFilterDto;
import ai.dto.own.request.filter.RoleFilterDto;
import ai.dto.own.response.RoleResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.RoleService;
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
@RequestMapping("/admin/roles")
@RestController
public class RoleController {
    RoleService roleService;

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
