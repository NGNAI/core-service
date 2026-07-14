package ai.exception;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import ai.dto.own.response.ForbiddenResponseDto;
import ai.dto.own.response.UnauthorizedResponseDto;
import ai.enums.ApiResponseStatus;
import ai.model.ApiResponseModel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    private ResponseEntity<ApiResponseModel<Void>> handlingException(Exception exception){
        log.error("Unexpected exception occurred!",exception);
        return buildResponse(ApiResponseStatus.UNEXPECTED);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    private ResponseEntity<ApiResponseModel<Void>> handlingHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException exception){
        log.error("Request method not allow!",exception);
        return buildResponse(ApiResponseStatus.REQUEST_METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    private ResponseEntity<ApiResponseModel<Void>> handlingHttpMessageNotReadableException(HttpMessageNotReadableException exception){
        log.error("Invalid request information!",exception);
        return buildResponse(ApiResponseStatus.INVALID_REQUEST_INFORMATION);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    private ResponseEntity<ApiResponseModel<Void>> handlingMethodArgumentNotValidException(MethodArgumentNotValidException exception){
        log.error("Request input invalid!",exception);
        ApiResponseStatus apiResponseStatus;

        try {
            apiResponseStatus = ApiResponseStatus.valueOf(Objects.requireNonNull(exception.getFieldError()).getDefaultMessage());
        } catch (IllegalArgumentException e) {
            apiResponseStatus = ApiResponseStatus.UNEXPECTED;
        }
        return buildResponse(apiResponseStatus);
    }

    @ExceptionHandler(AppException.class)
    private ResponseEntity<ApiResponseModel<Void>> handlingAppException(AppException appException){
        log.warn(appException.getMessage(),appException);
        return buildResponse(appException.getApiResponseStatus());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    private ResponseEntity<ApiResponseModel<Void>> handlingAuthorizationDeniedException(AuthorizationDeniedException exception){
        log.error("Permission denied!",exception);
        return buildResponse(ApiResponseStatus.PERMISSION_DENIED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    private ResponseEntity<ApiResponseModel<ForbiddenResponseDto>> handlingAccessDeniedException(AccessDeniedException exception){
        log.error("Access denied!",exception);
        ForbiddenResponseDto error = ForbiddenResponseDto.builder()
                .message("Insufficient permissions")
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponseModel.<ForbiddenResponseDto>builder()
                        .status(403)
                        .message("Forbidden")
                        .data(error)
                        .build());
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    private ResponseEntity<ApiResponseModel<UnauthorizedResponseDto>> handlingAuthenticationException(
            org.springframework.security.core.AuthenticationException exception){
        log.error("Authentication failed!",exception);
        UnauthorizedResponseDto error = UnauthorizedResponseDto.builder()
                .message("Authentication required")
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseModel.<UnauthorizedResponseDto>builder()
                        .status(401)
                        .message("Unauthorized")
                        .data(error)
                        .build());
    }

    private <T> ResponseEntity<ApiResponseModel<T>> buildResponse(ApiResponseStatus apiResponseEnum){
        return ResponseEntity
                .status(apiResponseEnum.getHttpStatusCode())
                .body(ApiResponseModel.<T>builder()
                        .status(apiResponseEnum.getCode())
                        .message(apiResponseEnum.getMessage())
                        .build());
    }
}