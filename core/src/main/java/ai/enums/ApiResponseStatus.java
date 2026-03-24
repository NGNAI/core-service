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
    USERNAME_CAN_NOT_BE_NULL_OR_EMPTY(1001, "Username cannot be null or empty", HttpStatus.BAD_REQUEST),
    PASSWORD_CAN_NOT_BE_NULL_OR_EMPTY(1002, "Password cannot be null or empty", HttpStatus.BAD_REQUEST),
    SOURCE_CAN_NOT_BE_NULL_OR_EMPTY(1003, "Source cannot be null or empty", HttpStatus.BAD_REQUEST),
    TOKEN_CAN_NOT_BE_NULL_OR_EMPTY(1004, "Token cannot be null or empty", HttpStatus.BAD_REQUEST),

    PARENT_ORGANIZATION_NOT_EXISTS(1005, "Parent organization does not exist", HttpStatus.NOT_FOUND),
    ORGANIZATION_NOT_EXISTS(1006, "Organization does not exist", HttpStatus.NOT_FOUND),
    ORGANIZATION_NOT_EMPTY(1007, "Organization is not empty", HttpStatus.CONFLICT),
    ORGANIZATION_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1008, "Organization name cannot be null or empty", HttpStatus.BAD_REQUEST),

    USER_NOT_EXISTS(1009, "User does not exist", HttpStatus.NOT_FOUND),
    USER_EXISTED(1010, "User already exists", HttpStatus.CONFLICT),

    ROLE_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1011, "Role name cannot be null or empty", HttpStatus.BAD_REQUEST),
    ROLE_NAME_EXISTED(1012, "Role name already exists", HttpStatus.CONFLICT),
    ROLE_ID_NOT_EXISTS(1013, "Role ID does not exist", HttpStatus.NOT_FOUND),
    ROLE_DEFAULT_ASSIGN_NOT_EXISTS(1014, "No role available for default assignment", HttpStatus.NOT_FOUND),
    ROLE_PERMISSION_CAN_NOT_BE_NULL(1015, "Role permissions cannot be null", HttpStatus.BAD_REQUEST),
    ROLE_ID_CAN_NOT_BE_NULL(1016, "Role ID cannot be null", HttpStatus.BAD_REQUEST),

    PERMISSION_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1017, "Permission name cannot be null or empty", HttpStatus.BAD_REQUEST),
    PERMISSION_NAME_EXISTED(1018, "Permission name already exists", HttpStatus.CONFLICT),
    PERMISSION_ID_NOT_EXISTS(1019, "Permission ID does not exist", HttpStatus.NOT_FOUND),

    USER_EXISTED_IN_ORGANIZATION(1020, "User already exists in organization", HttpStatus.CONFLICT),
    USER_WITH_ROLE_EXISTED_IN_ORGANIZATION(1021, "User with role already exists in organization", HttpStatus.CONFLICT),
    USER_WITH_ROLE_NOT_EXIST_IN_ORGANIZATION(1022, "User with role does not exist in organization", HttpStatus.NOT_FOUND),
    USER_NOT_EXIST_IN_ORGANIZATION(1023, "User does not exist in organization", HttpStatus.NOT_FOUND),
    USER_MUST_HAVE_AT_LEAST_ONE_ROLE_IN_ORGANIZATION(1024, "User must have at least one role in the organization", HttpStatus.BAD_REQUEST),

    USER_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1025, "User name cannot be null or empty", HttpStatus.BAD_REQUEST),
    USER_FIRST_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1026, "User first name cannot be null or empty", HttpStatus.BAD_REQUEST),
    USER_PASSWORD_CAN_NOT_BE_NULL_OR_EMPTY(1027, "User password cannot be null or empty", HttpStatus.BAD_REQUEST),
    USER_GENDER_VALUE_INVALID(1028, "User gender must be 0 (male) or 1 (female)", HttpStatus.BAD_REQUEST),
    USER_EMAIL_VALUE_INVALID(1029, "User email is invalid", HttpStatus.BAD_REQUEST),
    USER_EMAIL_CAN_NOT_BE_NULL_OR_EMPTY(1030, "Email cannot be null or empty", HttpStatus.BAD_REQUEST),

    USER_IDS_CAN_NOT_BE_NULL_OR_EMPTY(1031, "User IDs cannot be null or empty", HttpStatus.BAD_REQUEST),

    TOPIC_ID_NOT_EXISTS(1032, "Topic ID does not exist", HttpStatus.NOT_FOUND),
    TOPIC_TITLE_CAN_NOT_BE_NULL_OR_EMPTY(1033, "Topic title cannot be null or empty", HttpStatus.BAD_REQUEST),
    TOPIC_TYPE_CAN_NOT_BE_NULL_OR_EMPTY(1034, "Topic type cannot be null or empty", HttpStatus.BAD_REQUEST),

    MESSAGE_CAN_NOT_BE_NULL_OR_EMPTY(1035, "Message cannot be null or empty", HttpStatus.BAD_REQUEST),

    UNAUTHENTICATED(1036, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    PERMISSION_DENIED(1037, "You do not have permission", HttpStatus.FORBIDDEN),
    USER_NOT_IN_ORG(1038, "User is not in any organization", HttpStatus.FORBIDDEN),
    TOKEN_INVALID(1039, "Invalid token", HttpStatus.UNAUTHORIZED),
    AUTHENTICATE_FAILED(1040, "Authentication failed", HttpStatus.UNAUTHORIZED),
    REQUEST_METHOD_NOT_ALLOWED(1041, "Request method not allowed", HttpStatus.METHOD_NOT_ALLOWED),
    INVALID_REQUEST_INFORMATION(1042, "Invalid request information", HttpStatus.BAD_REQUEST),

    MEDIA_JOB_ID_NOT_EXISTS(1101, "Media job id not exists",HttpStatus.BAD_REQUEST),
    INGESTION_SERVICE_UNAVAILABLE(1102, "Ingestion service unavailable",HttpStatus.BAD_REQUEST),
    MEDIA_PARENT_NOT_EXISTS(1103, "Media parent not exists",HttpStatus.BAD_REQUEST),
    MEDIA_FILE_REQUIRED(1104, "Media file is required",HttpStatus.BAD_REQUEST),
    MEDIA_ACCESS_LEVEL_INVALID(1105, "Media access level is invalid",HttpStatus.BAD_REQUEST),
    MEDIA_TARGET_INVALID(1106, "Media target is invalid",HttpStatus.BAD_REQUEST),
    MEDIA_ORG_ID_REQUIRED(1107, "Media org id is required",HttpStatus.BAD_REQUEST),
    MEDIA_OWNER_ID_REQUIRED(1108, "Media owner id is required",HttpStatus.BAD_REQUEST),
    MEDIA_UNIT_REQUIRED(1109, "Media unit is required",HttpStatus.BAD_REQUEST),
    MEDIA_USERNAME_REQUIRED(1110, "Media username is required",HttpStatus.BAD_REQUEST),
    MEDIA_UPLOAD_FAILED(1111, "Media upload failed",HttpStatus.BAD_REQUEST),
    MEDIA_DOWNLOAD_FAILED(1121, "Media download failed",HttpStatus.BAD_REQUEST),
    MEDIA_NOT_EXISTS(1112, "Media not exists",HttpStatus.BAD_REQUEST),
    MEDIA_INGESTION_RETRY_ONLY_FAILED(1113, "Only failed media can retry ingestion",HttpStatus.BAD_REQUEST),
    MEDIA_NAME_REQUIRED(1114, "Media name is required",HttpStatus.BAD_REQUEST),
    MEDIA_INGESTION_RETRY_ONLY_INGESTION_TARGET(1115, "Only ingestion target media can retry",HttpStatus.BAD_REQUEST),
    MEDIA_SORT_BY_INVALID(1116, "Media sort field is invalid",HttpStatus.BAD_REQUEST),
    MEDIA_SORT_DIR_INVALID(1120, "Media sort direction is invalid",HttpStatus.BAD_REQUEST),
    MEDIA_FOLDER_ONLY_OPERATION(1116, "This operation is only for folder",HttpStatus.BAD_REQUEST),
    MEDIA_PARENT_MUST_BE_FOLDER(1117, "Media parent must be folder",HttpStatus.BAD_REQUEST),
    MEDIA_MOVE_CYCLE_NOT_ALLOWED(1118, "Media move cycle is not allowed",HttpStatus.BAD_REQUEST),
    MEDIA_FOLDER_UPDATE_REQUIRED(1119, "Media folder update payload is empty",HttpStatus.BAD_REQUEST),
    MEDIA_DELETE_FAILED(1122, "Media delete failed",HttpStatus.BAD_REQUEST),

    UNEXPECTED(9999, "An unexpected error occurred!",HttpStatus.INTERNAL_SERVER_ERROR);

    int code;
    String message;
    HttpStatusCode httpStatusCode;
}
