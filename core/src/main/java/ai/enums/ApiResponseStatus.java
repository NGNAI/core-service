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
    PARENT_ORGANIZATION_NOT_EXISTS(1001, "Parent organization not exists", HttpStatus.NOT_FOUND),
    ORGANIZATION_NOT_EXISTS(1002, "Organization not exists", HttpStatus.NOT_FOUND),
    ORGANIZATION_NOT_EMPTY(1003, "Organization not empty", HttpStatus.CONFLICT),
    ORGANIZATION_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1004, "Organization name can't be null or empty", HttpStatus.BAD_REQUEST),

    USER_NOT_EXISTS(1005, "User not exists", HttpStatus.NOT_FOUND),
    USER_EXISTED(1006, "User existed", HttpStatus.CONFLICT),

    ROLE_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1007, "Role name can't be null or empty", HttpStatus.BAD_REQUEST),
    ROLE_NAME_EXISTED(1008, "Role name existed", HttpStatus.CONFLICT),
    ROLE_ID_NOT_EXISTS(1009, "Role id not exists", HttpStatus.NOT_FOUND),
    ROLE_DEFAULT_ASSIGN_NOT_EXISTS(1010, "There are no role for assign default role", HttpStatus.NOT_FOUND),
    ROLE_PERMISSION_CAN_NOT_BE_NULL(1011, "Role permissions can't be null", HttpStatus.BAD_REQUEST),
    ROLE_ID_CAN_NOT_BE_NULL(1012, "Role id can't be null", HttpStatus.BAD_REQUEST),

    PERMISSION_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1013, "Permission name can't be null or empty", HttpStatus.BAD_REQUEST),
    PERMISSION_NAME_EXISTED(1014, "Permission name existed", HttpStatus.CONFLICT),
    PERMISSION_ID_NOT_EXISTS(1015, "Permission id not exists", HttpStatus.NOT_FOUND),

    USER_EXISTED_IN_ORGANIZATION(1016, "User already exist in organization", HttpStatus.CONFLICT),
    USER_WITH_ROLE_EXISTED_IN_ORGANIZATION(1017, "User with role already exist in organization", HttpStatus.CONFLICT),
    USER_WITH_ROLE_NOT_EXIST_IN_ORGANIZATION(1018, "User with role not exist in organization", HttpStatus.NOT_FOUND),
    USER_NOT_EXIST_IN_ORGANIZATION(1019, "User not exist in organization", HttpStatus.NOT_FOUND),
    USER_MUST_HAVE_AT_LEAST_ONE_ROLE_IN_ORGANIZATION(1020, "User must have at least one role in organization", HttpStatus.BAD_REQUEST),

    USER_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1021, "User name can't be null or empty", HttpStatus.BAD_REQUEST),
    USER_FIRST_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1022, "User first name can't be null or empty", HttpStatus.BAD_REQUEST),
    USER_PASSWORD_CAN_NOT_BE_NULL_OR_EMPTY(1023, "User password can't be null or empty", HttpStatus.BAD_REQUEST),
    USER_GENDER_VALUE_INVALID(1024, "User gender value only in 0 = male, 1 = female", HttpStatus.BAD_REQUEST),
    USER_EMAIL_VALUE_INVALID(1025, "User email is invalid", HttpStatus.BAD_REQUEST),

    USER_IDS_CAN_NOT_BE_NULL_OR_EMPTY(1026, "User ids can't be null or empty", HttpStatus.BAD_REQUEST),

    TOPIC_ID_NOT_EXISTS(1009, "Topic id not exists", HttpStatus.NOT_FOUND),
    TOPIC_TITLE_CAN_NOT_BE_NULL(1012, "Topic title can't be null or empty", HttpStatus.BAD_REQUEST),

    UNAUTHENTICATED(1027, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(1031, "Token invalid", HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS),
    AUTHENTICATE_FAILED(1028, "Authenticate failed!", HttpStatus.UNAUTHORIZED),
    REQUEST_METHOD_NOT_ALLOWED(1029, "Request method not allowed!", HttpStatus.METHOD_NOT_ALLOWED),
    INVALID_REQUEST_INFORMATION(1030, "Invalid request information!", HttpStatus.BAD_REQUEST),

    MEDIA_JOB_ID_NOT_EXISTS(1101, "Media job id not exists",HttpStatus.OK),
    INGESTION_SERVICE_UNAVAILABLE(1102, "Ingestion service unavailable",HttpStatus.OK),
    MEDIA_PARENT_NOT_EXISTS(1103, "Media parent not exists",HttpStatus.OK),
    MEDIA_FILE_REQUIRED(1104, "Media file is required",HttpStatus.OK),
    MEDIA_TARGET_INVALID(1105, "Media target is invalid",HttpStatus.OK),
    MEDIA_ORG_ID_REQUIRED(1106, "Media org id is required",HttpStatus.OK),
    MEDIA_OWNER_ID_REQUIRED(1107, "Media owner id is required",HttpStatus.OK),
    MEDIA_UNIT_REQUIRED(1108, "Media unit is required",HttpStatus.OK),
    MEDIA_USERNAME_REQUIRED(1109, "Media username is required",HttpStatus.OK),
    MEDIA_UPLOAD_FAILED(1110, "Media upload failed",HttpStatus.OK),
    MEDIA_DOWNLOAD_FAILED(1121, "Media download failed",HttpStatus.OK),
    MEDIA_NOT_EXISTS(1111, "Media not exists",HttpStatus.OK),
    MEDIA_INGESTION_RETRY_ONLY_FAILED(1112, "Only failed media can retry ingestion",HttpStatus.OK),
    MEDIA_NAME_REQUIRED(1113, "Media name is required",HttpStatus.OK),
    MEDIA_INGESTION_RETRY_ONLY_INGESTION_TARGET(1114, "Only ingestion target media can retry",HttpStatus.OK),
    MEDIA_SORT_BY_INVALID(1115, "Media sort field is invalid",HttpStatus.OK),
    MEDIA_SORT_DIR_INVALID(1120, "Media sort direction is invalid",HttpStatus.OK),
    MEDIA_FOLDER_ONLY_OPERATION(1116, "This operation is only for folder",HttpStatus.OK),
    MEDIA_PARENT_MUST_BE_FOLDER(1117, "Media parent must be folder",HttpStatus.OK),
    MEDIA_MOVE_CYCLE_NOT_ALLOWED(1118, "Media move cycle is not allowed",HttpStatus.OK),
    MEDIA_FOLDER_UPDATE_REQUIRED(1119, "Media folder update payload is empty",HttpStatus.OK),

    UNEXPECTED(9999, "An unexpected error occurred!",HttpStatus.OK);

    int code;
    String message;
    HttpStatusCode httpStatusCode;
}
