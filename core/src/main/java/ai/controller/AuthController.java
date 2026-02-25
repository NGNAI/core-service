package ai.controller;

import ai.dto.own.request.AuthRequestDto;
import ai.dto.own.request.IntrospectRequestDto;
import ai.dto.own.response.AuthResponseDto;
import ai.dto.own.response.IntrospectResponseDto;
import ai.model.ApiResponseModel;
import ai.service.AuthService;
import com.nimbusds.jose.JOSEException;
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
@RequestMapping("/pub/auth")
@RestController
public class AuthController {
    AuthService authService;

    @PostMapping("/introspect")
    ResponseEntity<ApiResponseModel<IntrospectResponseDto>> auth(@RequestBody IntrospectRequestDto introspectRequestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<IntrospectResponseDto>builder()
                        .message("Token validate!")
                        .data(authService.introspect(introspectRequestDto))
                        .build()
        );
    }

    @PostMapping
    ResponseEntity<ApiResponseModel<AuthResponseDto>> auth(@RequestBody AuthRequestDto authRequestDto) throws JOSEException {
        return ResponseEntity.ok(
                ApiResponseModel.<AuthResponseDto>builder()
                        .message("Authenticated successfully")
                        .data(authService.auth(authRequestDto))
                        .build()
        );
    }
}
