package ai.dto.own.request.audit;

import ai.enums.AuditAction;
import ai.enums.AuditResource;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLogRequest {
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
    String userAgent;
    String method;
    String path;
    boolean success;
    Map<String, Object> details;
    String errorMessage;
}
