package ai.controller;

import ai.dto.own.request.UserPasswordUpdateRequestDto;
import ai.dto.own.request.UserProfileUpdateRequestDto;
import ai.dto.own.response.UserProfileResponseDto;
import ai.model.ApiResponseModel;
import ai.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/profile")
@RestController
public class UserProfileController {
    UserProfileService userProfileService;

    @GetMapping()
    ResponseEntity<ApiResponseModel<UserProfileResponseDto>> getProfile(){
        return ResponseEntity.ok(
                ApiResponseModel.<UserProfileResponseDto>builder()
                        .message("Get profile successfully")
                        .data(userProfileService.getProfile())
                        .build()
        );
    }

    @PutMapping
    ResponseEntity<ApiResponseModel<UserProfileResponseDto>> update(@Valid @RequestBody UserProfileUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<UserProfileResponseDto>builder()
                        .message("Update profile successfully")
                        .data(userProfileService.update(requestDto))
                        .build()
        );
    }

    @PatchMapping("/password")
    ResponseEntity<ApiResponseModel<Void>> changePassword(@Valid @RequestBody UserPasswordUpdateRequestDto requestDto){
        userProfileService.changePassword(requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Change password successfully")
                        .build()
        );
    }
}
