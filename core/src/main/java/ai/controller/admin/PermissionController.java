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
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.request.PermissionCreateRequestDto;
import ai.dto.own.request.PermissionUpdateRequestDto;
import ai.dto.own.request.filter.PermissionFilterDto;
import ai.dto.own.response.PermissionResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.PermissionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/permissions")
@RestController
public class PermissionController {
    PermissionService permissionService;

    @GetMapping
    @PreAuthorize("@perm.canAccess(null, 'PERMISSION', 'READ', null)")
    ResponseEntity<ApiResponseModel<List<PermissionResponseDto>>> getAll(@Valid @ModelAttribute PermissionFilterDto filterDto){
        CustomPairModel<Long, List<PermissionResponseDto>> result = permissionService.getAll(filterDto);

        return ResponseEntity.ok(
                ApiResponseModel.<List<PermissionResponseDto>>builder()
                        .message("Get list permissions successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @PostMapping
    @PreAuthorize("@perm.canAccess(null, 'PERMISSION', 'CREATE', null)")
    ResponseEntity<ApiResponseModel<PermissionResponseDto>> create(@Valid @RequestBody PermissionCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<PermissionResponseDto>builder()
                        .message("Create permission successfully")
                        .data(permissionService.create(requestDto))
                        .build()
        );
    }

    @PutMapping("/{permissionId}")
    @PreAuthorize("@perm.canAccess(null, 'PERMISSION', 'UPDATE', null)")
    ResponseEntity<ApiResponseModel<PermissionResponseDto>> update(@PathVariable UUID permissionId, @Valid @RequestBody PermissionUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<PermissionResponseDto>builder()
                        .message("Update permission successfully")
                        .data(permissionService.update(permissionId, requestDto))
                        .build()
        );
    }

    @DeleteMapping("/{permissionId}")
    @PreAuthorize("@perm.canAccess(null, 'PERMISSION', 'DELETE', null)")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID permissionId){
        permissionService.delete(permissionId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete permission successfully")
                        .build()
        );
    }
}
