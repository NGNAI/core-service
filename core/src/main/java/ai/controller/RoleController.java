package ai.controller;

import ai.dto.own.request.RoleCreateRequestDto;
import ai.dto.own.request.RolePermissionUpdateRequestDto;
import ai.dto.own.request.RoleUpdateRequestDto;
import ai.dto.own.response.RoleResponseDto;
import ai.model.ApiResponseModel;
import ai.service.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/prv/role")
@RestController
public class RoleController {
    RoleService roleService;

    @GetMapping
    ResponseEntity<ApiResponseModel<List<RoleResponseDto>>> getAll(){
        return ResponseEntity.ok(
                ApiResponseModel.<List<RoleResponseDto>>builder()
                        .message("Get list roles successfully")
                        .data(roleService.getAll())
                        .build()
        );
    }

    @PostMapping
    ResponseEntity<ApiResponseModel<RoleResponseDto>> create(@RequestBody RoleCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<RoleResponseDto>builder()
                        .message("Create roles successfully")
                        .data(roleService.create(requestDto))
                        .build()
        );
    }

    @PutMapping("/{roleId}")
    ResponseEntity<ApiResponseModel<RoleResponseDto>> update(@PathVariable int roleId, @RequestBody RoleUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<RoleResponseDto>builder()
                        .message("Update Role successfully")
                        .data(roleService.update(roleId, requestDto))
                        .build()
        );
    }

    @PutMapping("/{roleId}/permissions")
    ResponseEntity<ApiResponseModel<RoleResponseDto>> updateRolePermission(@PathVariable int roleId, @RequestBody RolePermissionUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<RoleResponseDto>builder()
                        .message("Update role permissions successfully")
                        .data(roleService.updatePermissions(roleId, requestDto))
                        .build()
        );
    }

    @DeleteMapping("/{roleId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable int roleId){
        roleService.delete(roleId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete role successfully")
                        .build()
        );
    }
}
