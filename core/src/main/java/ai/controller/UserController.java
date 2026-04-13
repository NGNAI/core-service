package ai.controller;

import ai.dto.own.request.UserCreateRequestDto;
import ai.dto.own.request.UserUpdateRequestDto;
import ai.dto.own.request.filter.UserFilterDto;
import ai.dto.own.response.UserResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.UserService;
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
@RequestMapping("/admin/users")
@RestController
public class UserController {
    UserService userService;

    @GetMapping("/{userId}")
    ResponseEntity<ApiResponseModel<UserResponseDto>> getById(@PathVariable UUID userId){
        return ResponseEntity.ok(
                ApiResponseModel.<UserResponseDto>builder()
                        .message("Get user successfully")
                        .data(userService.getById(userId))
                        .build()
        );
    }

    @GetMapping
    ResponseEntity<ApiResponseModel<List<UserResponseDto>>> getAll(@Valid @ModelAttribute UserFilterDto filterDto){
        CustomPairModel<Long, List<UserResponseDto>> result = userService.getAll(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<UserResponseDto>>builder()
                        .message("Get list users successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @PostMapping
    ResponseEntity<ApiResponseModel<UserResponseDto>> create(@Valid @RequestBody UserCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<UserResponseDto>builder()
                        .message("Create user successfully")
                        .data(userService.create(requestDto))
                        .build()
        );
    }

    @PutMapping("/{userId}")
    ResponseEntity<ApiResponseModel<UserResponseDto>> update(@PathVariable UUID userId,@Valid @RequestBody UserUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<UserResponseDto>builder()
                        .message("Update user successfully")
                        .data(userService.update(userId, requestDto))
                        .build()
        );
    }


    @DeleteMapping("/{userId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID userId){
        userService.delete(userId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete user successfully")
                        .build()
        );
    }
}
