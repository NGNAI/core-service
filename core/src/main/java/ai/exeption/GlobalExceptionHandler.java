package ai.exeption;

import ai.enums.ApiResponseStatus;
import ai.model.ApiResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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

    /* App exception */
    @ExceptionHandler(AppException.class)
    private ResponseEntity<ApiResponseModel<Void>> handlingAppException(AppException appException){
        log.warn(appException.getMessage(),appException);
        return buildResponse(appException.getApiResponseStatus());
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
