package ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

import ai.dto.own.request.AuthRequestDto;
import ai.dto.own.request.IntrospectRequestDto;
import ai.dto.own.request.OrganizationSelectRequestDto;
import ai.dto.own.response.AuthResponseDto;
import ai.dto.own.response.IntrospectResponseDto;
import ai.dto.own.response.OrganizationSelectResponseDto;
import ai.model.ApiResponseModel;
import ai.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/auth")
@RestController
@Tag(name = "Authentication", description = "Authentication APIs for user login and token introspection")
public class AuthController {
    AuthService authService;

    @Operation(summary = "Introspect token", description = "Validate and introspect a JWT token")
    @PostMapping("/introspect")
    ResponseEntity<ApiResponseModel<IntrospectResponseDto>> introspect(@Valid @RequestBody IntrospectRequestDto introspectRequestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<IntrospectResponseDto>builder()
                        .message("Token validate!")
                        .data(authService.introspect(introspectRequestDto))
                        .build()
        );
    }

    @Operation(summary = "Authenticate user", description = "Authenticate user and return JWT token")
    @PostMapping
    ResponseEntity<ApiResponseModel<AuthResponseDto>> auth(@Valid @RequestBody AuthRequestDto authRequestDto, HttpServletResponse response) throws JOSEException, JsonProcessingException {
        AuthResponseDto authResponse = authService.auth(authRequestDto);

        // Cookie cookie = new Cookie("AUTH_TOKEN", authResponse.getToken());
        // cookie.setHttpOnly(true); // QUAN TRỌNG: Ngăn JavaScript truy cập (Chống XSS)
        // cookie.setSecure(false);   // CHÚ Ý: Bắt buộc chạy HTTPS (ở localhost có thể tạm để false)
        // cookie.setPath("/");       // Cookie có hiệu lực cho toàn bộ domain
        // cookie.setMaxAge(2592000);   // Thời gian sống (ví dụ: 1 tháng)

        // response.addCookie(cookie);

        return ResponseEntity.ok(
                ApiResponseModel.<AuthResponseDto>builder()
                        .message("Authenticated successfully")
                        .data(authResponse)
                        .build()
        );
    }

    @Operation(summary = "Select organization", description = "Select organization for the authenticated user")
    @PostMapping("/select-org")
    ResponseEntity<ApiResponseModel<OrganizationSelectResponseDto>> selectOrg(@Valid @RequestBody OrganizationSelectRequestDto selectRequestDto, HttpServletResponse response) throws JOSEException {
        OrganizationSelectResponseDto orgResponse = authService.selectOrg(selectRequestDto);

        // Cookie cookie = new Cookie("AUTH_TOKEN", orgResponse.getToken());
        // cookie.setHttpOnly(true); // QUAN TRỌNG: Ngăn JavaScript truy cập (Chống XSS)
        // cookie.setSecure(false);   // CHÚ Ý: Bắt buộc chạy HTTPS (ở localhost có thể tạm để false)
        // cookie.setPath("/");       // Cookie có hiệu lực cho toàn bộ domain
        // cookie.setMaxAge(2592000);   // Thời gian sống (ví dụ: 1 tháng)

        // response.addCookie(cookie);

        return ResponseEntity.ok(
                ApiResponseModel.<OrganizationSelectResponseDto>builder()
                        .message("Select organization successfully")
                        .data(orgResponse)
                        .build()
        );
    }
}
