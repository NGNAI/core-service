package ai.dto.own.response;

import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.enums.AuditStatus;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonPropertyOrder({
        "id",
        "action",
        "resource",
        "resourceId",
        "resourceName",
        "description",
        "userId",
        "userName",
        "orgId",
        "organizationName",
        "ipAddress",
        "method",
        "path",
        "status",
        "details",
        "errorMessage",
        "createdAt"
})
@Data
@FieldDefaults(level = AccessLevel.PROTECTED)
public class AuditLogResponseDto implements Serializable {
    UUID id;
    AuditAction action;
    AuditResource resource;
    String resourceId;
    String resourceName;
    String description;
    UUID userId;
    String userName;
    UUID orgId;
    String organizationName;
    String ipAddress;
    String method;
    String path;
    AuditStatus status;
    Map<String, Object> details;
    String errorMessage;
    Instant createdAt;
}
