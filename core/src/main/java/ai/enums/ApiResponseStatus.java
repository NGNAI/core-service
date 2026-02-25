package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ApiResponseStatus {
    USER_NOT_EXISTS(1001, "User not exists",HttpStatus.OK),
    USER_EXISTED(1002, "User existed",HttpStatus.OK),

    ROLE_NAME_EXISTED(1004, "Role name existed",HttpStatus.OK),
    ROLE_NAME_NOT_EXISTS(1005, "Role name not exists",HttpStatus.OK),

    PERMISSION_NAME_EXISTED(1006, "Permission name existed",HttpStatus.OK),
    PERMISSION_NAME_NOT_EXISTS(1007, "Permission name not exists",HttpStatus.OK),

    UNAUTHENTICATED(1011, "Unauthenticated",HttpStatus.OK),
    AUTHENTICATE_FAILED(1012, "Authenticate failed!",HttpStatus.OK),
    REQUEST_METHOD_NOT_ALLOWED(1013, "Request method not allowed!",HttpStatus.OK),

    UNEXPECTED(9999, "An unexpected error occurred!",HttpStatus.OK);

    int code;
    String message;
    HttpStatusCode httpStatusCode;
}
