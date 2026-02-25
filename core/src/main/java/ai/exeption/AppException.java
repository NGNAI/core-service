package ai.exeption;

import ai.enums.ApiResponseStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppException extends RuntimeException{
    ApiResponseStatus apiResponseStatus;

    public AppException(ApiResponseStatus apiResponseStatus) {
        super(apiResponseStatus.getMessage());
        this.apiResponseStatus = apiResponseStatus;
    }
}
