package ai.configuration;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import ai.model.ApiResponseModel;
import lombok.extern.slf4j.Slf4j;

/**
 * Global response advice to standardize all API responses.
 * Wraps all successful responses with ApiResponse and handles exceptions with ApiErrorResponse.
 */
@Slf4j
// @ControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        // Apply to all controllers
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        System.out.println("GlobalResponseAdvice: beforeBodyWrite called for body: " + body);

        if (body instanceof ApiResponseModel) {
            System.out.println("GlobalResponseAdvice: Body is already an instance of ApiResponseModel, returning as is.");
            return body;
        }

        return body; // Return the body as is, without wrapping
    }
    

    
}
