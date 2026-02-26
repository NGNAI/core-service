package ai.controller;

import ai.dto.own.request.PermissionCreateRequestDto;
import ai.dto.own.request.PermissionUpdateRequestDto;
import ai.dto.own.response.PermissionResponseDto;
import ai.model.ApiResponseModel;
import ai.service.PermissionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/prv/permission")
@RestController
public class PermissionController {
    PermissionService permissionService;

    @GetMapping
    ResponseEntity<ApiResponseModel<List<PermissionResponseDto>>> getAll(){
        return ResponseEntity.ok(
                ApiResponseModel.<List<PermissionResponseDto>>builder()
                        .message("Get list permissions successfully")
                        .data(permissionService.getAll())
                        .build()
        );
    }

    @PostMapping
    ResponseEntity<ApiResponseModel<PermissionResponseDto>> create(@RequestBody PermissionCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<PermissionResponseDto>builder()
                        .message("Create permission successfully")
                        .data(permissionService.create(requestDto))
                        .build()
        );
    }

    @PutMapping("/{permissionId}")
    ResponseEntity<ApiResponseModel<PermissionResponseDto>> update(@PathVariable int permissionId, @RequestBody PermissionUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<PermissionResponseDto>builder()
                        .message("Update permission successfully")
                        .data(permissionService.update(permissionId, requestDto))
                        .build()
        );
    }

    @DeleteMapping("/{permissionId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable int permissionId){
        permissionService.delete(permissionId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete permission successfully")
                        .build()
        );
    }
}
