package ai.controller;

import ai.dto.own.request.AuthRequestDto;
import ai.dto.own.request.IntrospectRequestDto;
import ai.dto.own.request.OrganizationSelectRequestDto;
import ai.dto.own.response.AuthResponseDto;
import ai.dto.own.response.IntrospectResponseDto;
import ai.dto.own.response.OrganizationSelectResponseDto;
import ai.model.ApiResponseModel;
import ai.service.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/auth")
@RestController
public class AuthController {
    AuthService authService;

    @PostMapping("/introspect")
    ResponseEntity<ApiResponseModel<IntrospectResponseDto>> introspect(@Valid @RequestBody IntrospectRequestDto introspectRequestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<IntrospectResponseDto>builder()
                        .message("Token validate!")
                        .data(authService.introspect(introspectRequestDto))
                        .build()
        );
    }

    @PostMapping
    ResponseEntity<ApiResponseModel<AuthResponseDto>> auth(@Valid @RequestBody AuthRequestDto authRequestDto) throws JOSEException, JsonProcessingException {
        return ResponseEntity.ok(
                ApiResponseModel.<AuthResponseDto>builder()
                        .message("Authenticated successfully")
                        .data(authService.auth(authRequestDto))
                        .build()
        );
    }

    @PostMapping("/select-org")
    ResponseEntity<ApiResponseModel<OrganizationSelectResponseDto>> selectOrg(@Valid @RequestBody OrganizationSelectRequestDto selectRequestDto) throws JOSEException {
        return ResponseEntity.ok(
                ApiResponseModel.<OrganizationSelectResponseDto>builder()
                        .message("Select organization successfully")
                        .data(authService.selectOrg(selectRequestDto))
                        .build()
        );
    }
}
