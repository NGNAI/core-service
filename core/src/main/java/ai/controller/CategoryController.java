package ai.controller;

import ai.dto.own.response.PermissionActionResponseDto;
import ai.dto.own.response.PermissionResourceResponseDto;
import ai.dto.own.response.PermissionScopeResponseDto;
import ai.model.ApiResponseModel;
import ai.service.CategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/category")
@RestController
public class CategoryController {
    CategoryService categoryService;

    @GetMapping("/rag-scope")
    ResponseEntity<ApiResponseModel<List<PermissionResourceResponseDto>>> ragScope() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<PermissionResourceResponseDto>>builder()
                        .message("Get rag scope successfully!")
                        .data(categoryService.getRagScope())
                        .build()
        );
    }

    @GetMapping("/permission-resource")
    ResponseEntity<ApiResponseModel<List<PermissionResourceResponseDto>>> permissionResource() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<PermissionResourceResponseDto>>builder()
                        .message("Get permission resource successfully!")
                        .data(categoryService.getPermissionResource())
                        .build()
        );
    }

    @GetMapping("/permission-action")
    ResponseEntity<ApiResponseModel<List<PermissionActionResponseDto>>> permissionAction() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<PermissionActionResponseDto>>builder()
                        .message("Get permission action successfully!")
                        .data(categoryService.getPermissionAction())
                        .build()
        );
    }

    @GetMapping("/permission-scope")
    ResponseEntity<ApiResponseModel<List<PermissionScopeResponseDto>>> permissionScope() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<PermissionScopeResponseDto>>builder()
                        .message("Get permission scope category successfully")
                        .data(categoryService.getPermissionScope())
                        .build()
        );
    }
}
