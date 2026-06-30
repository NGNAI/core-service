package ai.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.JOSEException;

import ai.dto.own.request.OrganizationSelectRequestDto;
import ai.dto.own.request.UserPasswordUpdateRequestDto;
import ai.dto.own.request.UserProfileUpdateRequestDto;
import ai.dto.own.response.OrganizationSelectResponseDto;
import ai.dto.own.response.OrganizationWithUserRoleDto;
import ai.dto.own.response.UserProfileResponseDto;
import ai.model.ApiResponseModel;
import ai.service.AuthService;
import ai.service.UserProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/profile")
@Tag(name = "User Profile", description = "User profile APIs")
@RestController
public class UserProfileController {
    UserProfileService userProfileService;
    AuthService authService;

    @Operation(summary = "Get user profile", description = "Retrieve the current user's profile information")
    @GetMapping()
    ResponseEntity<ApiResponseModel<UserProfileResponseDto>> getProfile(){
        return ResponseEntity.ok(
                ApiResponseModel.<UserProfileResponseDto>builder()
                        .message("Get profile successfully")
                        .data(userProfileService.getProfile())
                        .build()
        );
    }

    @Operation(summary = "Update user profile", description = "Update the current user's profile with provided data")
    @PutMapping
    ResponseEntity<ApiResponseModel<UserProfileResponseDto>> update(@Valid @RequestBody UserProfileUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<UserProfileResponseDto>builder()
                        .message("Update profile successfully")
                        .data(userProfileService.update(requestDto))
                        .build()
        );
    }

    @Operation(summary = "Change user password", description = "Change the password for the current user")
    @PatchMapping("/password")
    ResponseEntity<ApiResponseModel<Void>> changePassword(@Valid @RequestBody UserPasswordUpdateRequestDto requestDto){
        userProfileService.changePassword(requestDto);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Change password successfully")
                        .build()
        );
    }

    @Operation(summary = "Change user organization", description = "Switch the current user's organization")
    @PostMapping("/change-org")
    ResponseEntity<ApiResponseModel<OrganizationSelectResponseDto>> changeOrg(@Valid @RequestBody OrganizationSelectRequestDto selectRequestDto) throws JOSEException {
        return ResponseEntity.ok(
                ApiResponseModel.<OrganizationSelectResponseDto>builder()
                        .message("Change organization successfully")
                        .data(authService.selectOrg(selectRequestDto))
                        .build()
        );
    }

    @Operation(summary = "List user organizations", description = "Retrieve a list of organizations the user belongs to with roles")
    @GetMapping("/list-org")
    ResponseEntity<ApiResponseModel<List<OrganizationWithUserRoleDto>>> listOrg(){
        return ResponseEntity.ok(
                ApiResponseModel.<List<OrganizationWithUserRoleDto>>builder()
                        .message("Get organization list successfully")
                        .data(userProfileService.listOrg())
                        .build()
        );
    }

}
