package ai.enums;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ApiResponseStatus {

    // ========================================================================
    // INVALID - 1001..1002
    // ========================================================================
    INVALID_SORT_FIELD_VALUE(1001, "Invalid sort field value", HttpStatus.BAD_REQUEST),
    INVALID_SORT_DIR_VALUE(1002, "Sort direction value only accept ASC/DESC", HttpStatus.BAD_REQUEST),

    // ========================================================================
    // AUTH / TOKEN - 1003..1010
    // ========================================================================
    USERNAME_CAN_NOT_BE_NULL_OR_EMPTY(1003, "Username cannot be null or empty", HttpStatus.BAD_REQUEST),
    PASSWORD_CAN_NOT_BE_NULL_OR_EMPTY(1004, "Password cannot be null or empty", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1005, "Password length must be at least 5 characters", HttpStatus.BAD_REQUEST),
    USER_SOURCE_CAN_NOT_BE_NULL_OR_EMPTY(1006, "Source cannot be null or empty", HttpStatus.BAD_REQUEST),
    TOKEN_CAN_NOT_BE_NULL_OR_EMPTY(1007, "Token cannot be null or empty", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1008, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(1009, "Invalid token", HttpStatus.UNAUTHORIZED),
    AUTHENTICATE_FAILED(1010, "Authentication failed", HttpStatus.UNAUTHORIZED),

    // ========================================================================
    // PERMISSION - 1011..1021
    // ========================================================================
    PERMISSION_DENIED(1011, "You do not have permission", HttpStatus.FORBIDDEN),
    PERMISSION_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1012, "Permission name cannot be null or empty", HttpStatus.BAD_REQUEST),
    PERMISSION_RESOURCE_CAN_NOT_BE_NULL_OR_EMPTY(1013, "Permission resource cannot be null or empty", HttpStatus.BAD_REQUEST),
    INVALID_VALUE_PERMISSION_RESOURCE(1014, "Invalid permission resource", HttpStatus.BAD_REQUEST),
    PERMISSION_ACTION_CAN_NOT_BE_NULL_OR_EMPTY(1015, "Permission action cannot be null or empty", HttpStatus.BAD_REQUEST),
    INVALID_VALUE_PERMISSION_ACTION(1016, "Invalid permission action", HttpStatus.BAD_REQUEST),
    PERMISSION_NAME_EXISTED(1017, "Permission name already exists", HttpStatus.CONFLICT),
    PERMISSION_ID_NOT_EXISTS(1018, "Permission ID does not exist", HttpStatus.NOT_FOUND),
    INVALID_VALUE_PERMISSION_SCOPE(1019, "Invalid permission scope", HttpStatus.BAD_REQUEST),
    ASSIGN_PERMISSION_ID_CAN_NOT_BE_NULL(1020, "Assign permission id cannot be null or empty", HttpStatus.BAD_REQUEST),
    ASSIGN_PERMISSION_SCOPE_CAN_NOT_BE_NULL(1021, "Assign permission scope cannot be null or empty", HttpStatus.BAD_REQUEST),

    // ========================================================================
    // ROLE - 1022..1027
    // ========================================================================
    ROLE_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1022, "Role name cannot be null or empty", HttpStatus.BAD_REQUEST),
    ROLE_NAME_EXISTED(1023, "Role name already exists", HttpStatus.CONFLICT),
    ROLE_ID_NOT_EXISTS(1024, "Role ID does not exist", HttpStatus.NOT_FOUND),
    ROLE_DEFAULT_ASSIGN_NOT_EXISTS(1025, "No role available for default assignment", HttpStatus.NOT_FOUND),
    ROLE_PERMISSION_CAN_NOT_BE_NULL(1026, "Role permissions cannot be null", HttpStatus.BAD_REQUEST),
    ROLE_ID_CAN_NOT_BE_NULL(1027, "Role ID cannot be null", HttpStatus.BAD_REQUEST),

    // ========================================================================
    // ORGANIZATION - 1028..1036
    // ========================================================================
    PARENT_ORGANIZATION_NOT_EXISTS(1028, "Parent organization does not exist", HttpStatus.NOT_FOUND),
    ORGANIZATION_NOT_EXISTS(1029, "Organization does not exist", HttpStatus.NOT_FOUND),
    ORGANIZATION_NOT_EMPTY(1030, "Organization is not empty", HttpStatus.CONFLICT),
    ORGANIZATION_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1031, "Organization name cannot be null or empty", HttpStatus.BAD_REQUEST),
    ORGANIZATION_ID_CAN_NOT_BE_NULL_OR_EMPTY(1032, "Organization id cannot be null or empty", HttpStatus.BAD_REQUEST),
    PARENT_ORGANIZATION_ID_CAN_NOT_BE_NULL_OR_EMPTY(1033, "Parent organization id cannot be null or empty", HttpStatus.BAD_REQUEST),
    ROOT_ORGANIZATION_NOT_EXIST(1034, "Root organization not exists!", HttpStatus.NOT_FOUND),
    NESTED_CHILD_VALUE_INVALID(1035, "Invalid nested child value!", HttpStatus.NOT_FOUND),
    ORG_ID_REQUIRED(1036, "Organization ID is required", HttpStatus.BAD_REQUEST),

    // ========================================================================
    // USER - 1037..1056
    // ========================================================================
    USER_NOT_EXISTS(1037, "User does not exist", HttpStatus.NOT_FOUND),
    USER_EXISTED(1038, "User already exists", HttpStatus.CONFLICT),
    USER_EXISTED_IN_ORGANIZATION(1039, "User already exists in organization", HttpStatus.CONFLICT),
    USER_WITH_ROLE_EXISTED_IN_ORGANIZATION(1040, "User with role already exists in organization", HttpStatus.CONFLICT),
    USER_WITH_ROLE_NOT_EXIST_IN_ORGANIZATION(1041, "User with role does not exist in organization", HttpStatus.NOT_FOUND),
    USER_NOT_EXIST_IN_ORGANIZATION(1042, "User does not exist in organization", HttpStatus.NOT_FOUND),
    USER_MUST_HAVE_AT_LEAST_ONE_ROLE_IN_ORGANIZATION(1043, "User must have at least one role in the organization", HttpStatus.BAD_REQUEST),
    USER_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1044, "User name cannot be null or empty", HttpStatus.BAD_REQUEST),
    USER_FIRST_NAME_CAN_NOT_BE_NULL_OR_EMPTY(1045, "User first name cannot be null or empty", HttpStatus.BAD_REQUEST),
    USER_PASSWORD_CAN_NOT_BE_NULL_OR_EMPTY(1046, "User password cannot be null or empty", HttpStatus.BAD_REQUEST),
    USER_PASSWORD_INCORRECT(1047, "User password incorrect", HttpStatus.BAD_REQUEST),
    USER_GENDER_VALUE_INVALID(1048, "User gender must be 0 (male) or 1 (female)", HttpStatus.BAD_REQUEST),
    USER_EMAIL_VALUE_INVALID(1049, "User email is invalid", HttpStatus.BAD_REQUEST),
    USER_PHONE_NUMBER_VALUE_INVALID(1050, "User phone number is invalid", HttpStatus.BAD_REQUEST),
    USER_EMAIL_CAN_NOT_BE_NULL_OR_EMPTY(1051, "Email cannot be null or empty", HttpStatus.BAD_REQUEST),
    ONLY_LOCAL_USER_CAN_UPDATE_INFO(1052, "Only local user can update", HttpStatus.BAD_REQUEST),
    USER_IDS_CAN_NOT_BE_NULL_OR_EMPTY(1053, "User IDs cannot be null or empty", HttpStatus.BAD_REQUEST),
    ROOT_USER_NOT_EXIST(1054, "Root user not exists!", HttpStatus.NOT_FOUND),
    USER_NOT_IN_ORG(1055, "User is not in any organization", HttpStatus.FORBIDDEN),
    INVALID_USER_SOURCE_VALUE(1056, "Invalid user source value", HttpStatus.BAD_REQUEST),

    // ========================================================================
    // TOPIC - 1057..1062
    // ========================================================================
    TOPIC_ID_NOT_EXISTS(1057, "Topic ID does not exist", HttpStatus.NOT_FOUND),
    TOPIC_TITLE_CAN_NOT_BE_NULL_OR_EMPTY(1058, "Topic title cannot be null or empty", HttpStatus.BAD_REQUEST),
    TOPIC_TYPE_CAN_NOT_BE_NULL_OR_EMPTY(1059, "Topic type cannot be null or empty", HttpStatus.BAD_REQUEST),
    TOPIC_SOURCE_ALREADY_EXISTS(1060, "Topic source already exists", HttpStatus.CONFLICT),
    TOPIC_SOURCE_NOT_EXISTS(1061, "Topic source does not exist", HttpStatus.NOT_FOUND),
    TOPIC_SOURCE_PAYLOAD_REQUIRED(1062, "Topic source payload must include file(s)", HttpStatus.BAD_REQUEST),

    // ========================================================================
    // NOTEBOOK - 1063..1070
    // ========================================================================
    NOTEBOOK_ID_NOT_EXISTS(1063, "Notebook ID does not exist", HttpStatus.NOT_FOUND),
    NOTEBOOK_TITLE_CAN_NOT_BE_NULL_OR_EMPTY(1064, "Notebook title cannot be null or empty", HttpStatus.BAD_REQUEST),
    NOTEBOOK_SOURCE_ALREADY_EXISTS(1065, "Notebook source already exists", HttpStatus.CONFLICT),
    NOTEBOOK_SOURCE_NOT_EXISTS(1066, "Notebook source does not exist", HttpStatus.NOT_FOUND),
    NOTEBOOK_SOURCE_PAYLOAD_REQUIRED(1067, "Notebook source payload must include file(s), textContent, or noteId", HttpStatus.BAD_REQUEST),
    NOTEBOOK_SOURCE_JOB_ID_NOT_EXISTS(1068, "Notebook source job id not exists", HttpStatus.BAD_REQUEST),
    NOTEBOOK_SOURCE_DELETE_IN_PROGRESS(1069, "Notebook source deletion is in progress", HttpStatus.CONFLICT),
    NOTEBOOK_SOURCE_DELETE_FAILED(1070, "Notebook source delete failed", HttpStatus.BAD_REQUEST),

    // ========================================================================
    // NOTE - 1071..1081
    // ========================================================================
    NOTE_NOT_EXISTS(1071, "Note does not exist", HttpStatus.NOT_FOUND),
    NOTE_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY(1072, "Note content cannot be null or empty", HttpStatus.BAD_REQUEST),
    NOTE_SOURCE_TYPE_CAN_NOT_BE_NULL_OR_EMPTY(1073, "Note source type cannot be null or empty", HttpStatus.BAD_REQUEST),
    NOTE_SOURCE_TYPE_INVALID(1074, "Invalid note source type", HttpStatus.BAD_REQUEST),
    NOTE_SOURCE_TARGET_INVALID(1075, "Note source target is invalid", HttpStatus.BAD_REQUEST),
    NOTE_TOPIC_ID_REQUIRED(1076, "Topic id is required for TOPIC note source", HttpStatus.BAD_REQUEST),
    NOTE_NOTEBOOK_ID_REQUIRED(1077, "Notebook id is required for NOTEBOOK note source", HttpStatus.BAD_REQUEST),
    NOTE_SORT_BY_INVALID(1078, "Note sort field is invalid", HttpStatus.BAD_REQUEST),
    NOTE_SORT_DIR_INVALID(1079, "Note sort direction is invalid", HttpStatus.BAD_REQUEST),
    NOTE_SOURCE_BY_INVALID(1080, "Invalid note source by", HttpStatus.BAD_REQUEST),
    NOTE_SOURCE_BY_NOT_ALLOW_UPDATE(1081, "Note source by not allow update", HttpStatus.BAD_REQUEST),

    // ========================================================================
    // MESSAGE - 1082..1091
    // ========================================================================
    INVALID_MESSAGE_PARENT_VALUE(1082, "Invalid message parent value", HttpStatus.BAD_REQUEST),
    INVALID_MESSAGE_PARENT_TYPE_CAN_NOT_BE_NULL_OR_EMPTY(1083, "Message parent type cannot be null or empty", HttpStatus.BAD_REQUEST),
    MESSAGE_PARENT_ID_CAN_NOT_BE_NULL_OR_EMPTY(1084, "Message parent id cannot be null or empty", HttpStatus.BAD_REQUEST),
    MESSAGE_TYPE_CAN_NOT_BE_NULL_OR_EMPTY(1085, "Message type cannot be null or empty", HttpStatus.BAD_REQUEST),
    MESSAGE_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY(1086, "Message content cannot be null or empty", HttpStatus.BAD_REQUEST),
    MESSAGE_ID_NOT_EXISTS(1087, "Message ID does not exist", HttpStatus.NOT_FOUND),
    MESSAGE_CAN_NOT_BE_NULL_OR_EMPTY(1088, "Message cannot be null or empty", HttpStatus.BAD_REQUEST),
    MESSAGE_FEEDBACK_NO_CHANGE(1089, "Message feedback has no change", HttpStatus.BAD_REQUEST),
    MESSAGE_FEEDBACK_CAN_NOT_BE_NULL_OR_EMPTY(1090, "Message feedback cannot be null or empty", HttpStatus.BAD_REQUEST),
    INVALID_MESSAGE_FEEDBACK_VALUE(1091, "Invalid message feedback value", HttpStatus.BAD_REQUEST),

    // ========================================================================
    // DATA_INGESTION - 1092..1117
    // ========================================================================
    DATA_INGESTION_JOB_ID_NOT_EXISTS(1092, "Data ingestion job id not exists", HttpStatus.BAD_REQUEST),
    INGESTION_SERVICE_UNAVAILABLE(1093, "Ingestion service unavailable", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_PARENT_NOT_EXISTS(1094, "Data ingestion parent not exists", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_FILE_REQUIRED(1095, "Data ingestion file is required", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_ACCESS_LEVEL_INVALID(1096, "Data ingestion access level is invalid", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_TARGET_INVALID(1097, "Data ingestion target is invalid", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_ORG_ID_REQUIRED(1098, "Data ingestion org id is required", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_OWNER_ID_REQUIRED(1099, "Data ingestion owner id is required", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_UNIT_REQUIRED(1100, "Data ingestion unit is required", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_USERNAME_REQUIRED(1101, "Data ingestion username is required", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_UPLOAD_FAILED(1102, "Data ingestion upload failed", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_DOWNLOAD_FAILED(1103, "Data ingestion download failed", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_NOT_EXISTS(1104, "Data ingestion not exists", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_RETRY_ONLY_FAILED(1105, "Only failed data ingestion can retry ingestion", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_NAME_REQUIRED(1106, "Data ingestion name is required", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_FROM_SOURCE_INVALID(1107, "Data ingestion from source is invalid", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_RETRY_ONLY_INGESTION_TARGET(1108, "Only ingestion target data ingestion can retry", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_SORT_BY_INVALID(1109, "Data ingestion sort field is invalid", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_SORT_DIR_INVALID(1110, "Data ingestion sort direction is invalid", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_FOLDER_ONLY_OPERATION(1111, "This operation is only for folder", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_PARENT_MUST_BE_FOLDER(1112, "Data ingestion parent must be folder", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_MOVE_CYCLE_NOT_ALLOWED(1113, "Data ingestion move cycle is not allowed", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_FOLDER_UPDATE_REQUIRED(1114, "Data ingestion folder update payload is empty", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_DELETE_FAILED(1115, "Data ingestion delete failed", HttpStatus.BAD_REQUEST),
    DATA_INGESTION_DELETE_IN_PROGRESS(1116, "Data ingestion deletion is in progress", HttpStatus.CONFLICT),
    DATA_INGESTION_NOT_COMPLETED(1117, "Data ingestion must be completed before this operation", HttpStatus.CONFLICT),

    // ========================================================================
    // DRIVE - 1118..1131
    // ========================================================================
    DRIVE_PARENT_NOT_EXISTS(1118, "Drive parent not exists", HttpStatus.BAD_REQUEST),
    DRIVE_FILE_REQUIRED(1119, "Drive file is required", HttpStatus.BAD_REQUEST),
    DRIVE_UPLOAD_FAILED(1120, "Drive upload failed", HttpStatus.BAD_REQUEST),
    DRIVE_DOWNLOAD_FAILED(1121, "Drive download failed", HttpStatus.BAD_REQUEST),
    DRIVE_NOT_EXISTS(1122, "Drive not exists", HttpStatus.BAD_REQUEST),
    DRIVE_NAME_REQUIRED(1123, "Drive name is required", HttpStatus.BAD_REQUEST),
    DRIVE_SORT_BY_INVALID(1124, "Drive sort field is invalid", HttpStatus.BAD_REQUEST),
    DRIVE_SORT_DIR_INVALID(1125, "Drive sort direction is invalid", HttpStatus.BAD_REQUEST),
    DRIVE_FOLDER_ONLY_OPERATION(1126, "This operation is only for drive folder", HttpStatus.BAD_REQUEST),
    DRIVE_PARENT_MUST_BE_FOLDER(1127, "Drive parent must be folder", HttpStatus.BAD_REQUEST),
    DRIVE_MOVE_CYCLE_NOT_ALLOWED(1128, "Drive move cycle is not allowed", HttpStatus.BAD_REQUEST),
    DRIVE_FOLDER_UPDATE_REQUIRED(1129, "Drive folder update payload is empty", HttpStatus.BAD_REQUEST),
    DRIVE_DELETE_FAILED(1130, "Drive delete failed", HttpStatus.BAD_REQUEST),
    DRIVE_DELETE_IN_PROGRESS(1131, "Drive deletion is in progress", HttpStatus.CONFLICT),

    // ========================================================================
    // ATTACHMENT - 1132..1137
    // ========================================================================
    ATTACHMENT_NOT_EXISTS(1132, "Attachment does not exist", HttpStatus.NOT_FOUND),
    ATTACHMENT_FILE_REQUIRED(1133, "Attachment file is required", HttpStatus.BAD_REQUEST),
    ATTACHMENT_UPLOAD_FAILED(1134, "Attachment upload failed", HttpStatus.BAD_REQUEST),
    ATTACHMENT_DOWNLOAD_FAILED(1135, "Attachment download failed", HttpStatus.BAD_REQUEST),
    ATTACHMENT_SORT_BY_INVALID(1136, "Attachment sort field is invalid", HttpStatus.BAD_REQUEST),
    ATTACHMENT_SORT_DIR_INVALID(1137, "Attachment sort direction is invalid", HttpStatus.BAD_REQUEST),

    // ========================================================================
    // DRAFT - 1138..1150
    // ========================================================================
    DRAFT_ID_NOT_EXISTS(1138, "Draft does not exist", HttpStatus.NOT_FOUND),
    DRAFT_TYPE_CAN_NOT_BE_NULL_OR_EMPTY(1139, "Draft type cannot be null or empty", HttpStatus.BAD_REQUEST),
    DRAFT_TITLE_CAN_NOT_BE_NULL_OR_EMPTY(1140, "Draft title cannot be null or empty", HttpStatus.BAD_REQUEST),
    DRAFT_DESCRIPTION_CAN_NOT_BE_NULL_OR_EMPTY(1141, "Draft description cannot be null or empty", HttpStatus.BAD_REQUEST),
    DRAFT_PRESENTATION_STYLE_CAN_NOT_BE_NULL_OR_EMPTY(1142, "Draft presentation style cannot be null or empty", HttpStatus.BAD_REQUEST),
    DRAFT_LANGUAGE_CAN_NOT_BE_NULL_OR_EMPTY(1143, "Draft language cannot be null or empty", HttpStatus.BAD_REQUEST),
    DRAFT_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY(1144, "Draft content cannot be null or empty", HttpStatus.BAD_REQUEST),
    DRAFT_GENERATION_FAILED(1145, "Draft generation failed", HttpStatus.BAD_GATEWAY),
    INVALID_DRAFT_TYPE_VALUE(1146, "Invalid draft type value", HttpStatus.BAD_REQUEST),
    DRAFT_VERSION_NOT_EXISTS(1147, "Draft version does not exist", HttpStatus.NOT_FOUND),
    DRAFT_ROLLBACK_REASON_CAN_NOT_BE_NULL_OR_EMPTY(1148, "Draft rollback reason cannot be null or empty", HttpStatus.BAD_REQUEST),
    DRAFT_LAST_VERSION_CAN_NOT_BE_DELETED(1149, "Cannot delete the last draft version", HttpStatus.CONFLICT),
    DRAFT_CONTENT_GENERATION_FAILED(1150, "Draft content generation failed", HttpStatus.BAD_GATEWAY),

    // ========================================================================
    // SETTING - 1151..1155
    // ========================================================================
    SETTING_KEY_NOT_EXISTS(1151, "Setting key does not exist", HttpStatus.NOT_FOUND),
    SETTING_KEY_EXISTED(1152, "Setting key already exists", HttpStatus.CONFLICT),
    SETTING_KEY_CAN_NOT_BE_NULL_OR_EMPTY(1153, "Setting key cannot be null or empty", HttpStatus.BAD_REQUEST),
    SETTING_TYPE_CAN_NOT_BE_NULL_OR_EMPTY(1154, "Setting type cannot be null or empty", HttpStatus.BAD_REQUEST),
    SETTING_GROUP_CAN_NOT_BE_NULL_OR_EMPTY(1155, "Setting group cannot be null or empty", HttpStatus.BAD_REQUEST),

    // ========================================================================
    // OTHER - 1156..1161
    // ========================================================================
    RAG_SCOPE_CAN_NOT_BE_NULL_OR_EMPTY(1156, "Scope cannot be null or empty", HttpStatus.BAD_REQUEST),
    INVALID_RAG_SCOPE_VALUE(1157, "Invalid scope value", HttpStatus.BAD_REQUEST),
    UNAVAILABLE_FOR_LEGAL_REASONS(1158, "Unavailable for legal reasons", HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS),
    REQUEST_METHOD_NOT_ALLOWED(1159, "Request method not allowed", HttpStatus.METHOD_NOT_ALLOWED),
    INVALID_REQUEST_INFORMATION(1160, "Invalid request information", HttpStatus.BAD_REQUEST),
    CHANGE_REQUEST_MESSAGE_CAN_NOT_BE_NULL_OR_EMPTY(1161, "Change request message cannot be null or empty", HttpStatus.BAD_REQUEST),

    // ========================================================================
    // UNEXPECTED - 9999
    // ========================================================================
    UNEXPECTED(9999, "An unexpected error occurred!", HttpStatus.INTERNAL_SERVER_ERROR);

    int code;
    String message;
    HttpStatusCode httpStatusCode;
}
 