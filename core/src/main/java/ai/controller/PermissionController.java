package ai.controller;

import ai.dto.own.request.PermissionCreateRequestDto;
import ai.dto.own.request.PermissionUpdateRequestDto;
import ai.dto.own.request.filter.PermissionFilterDto;
import ai.dto.own.response.PermissionResponseDto;
import ai.dto.own.response.UserResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.PermissionService;
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
@RequestMapping("/admin/permissions")
@RestController
public class PermissionController {
    PermissionService permissionService;

    @GetMapping
    ResponseEntity<ApiResponseModel<List<PermissionResponseDto>>> getAll(@ModelAttribute PermissionFilterDto filterDto){
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
    ResponseEntity<ApiResponseModel<PermissionResponseDto>> create(@Valid @RequestBody PermissionCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<PermissionResponseDto>builder()
                        .message("Create permission successfully")
                        .data(permissionService.create(requestDto))
                        .build()
        );
    }

    @PutMapping("/{permissionId}")
    ResponseEntity<ApiResponseModel<PermissionResponseDto>> update(@PathVariable UUID permissionId, @Valid @RequestBody PermissionUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<PermissionResponseDto>builder()
                        .message("Update permission successfully")
                        .data(permissionService.update(permissionId, requestDto))
                        .build()
        );
    }

    @DeleteMapping("/{permissionId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID permissionId){
        permissionService.delete(permissionId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete permission successfully")
                        .build()
        );
    }
}
