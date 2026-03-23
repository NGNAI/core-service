package ai.util;

import ai.enums.ApiResponseStatus;
import ai.model.ApiResponseModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import org.springframework.http.MediaType;

import java.io.IOException;

@UtilityClass
public class ServletUtil {
    public void makeResponse(HttpServletResponse response, ApiResponseStatus responseStatus) throws IOException {
        response.setStatus(responseStatus.getHttpStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponseModel<Object> apiResponse = ApiResponseModel.builder()
                .status(responseStatus.getCode())
                .message(responseStatus.getMessage())
                .build();

        ObjectMapper objectMapper = new ObjectMapper();

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        response.flushBuffer();
    }
}
