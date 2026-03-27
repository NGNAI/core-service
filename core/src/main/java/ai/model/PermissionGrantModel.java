package ai.model;

import ai.enums.PermissionAction;
import ai.enums.PermissionResource;
import ai.enums.PermissionScope;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionGrantModel {
    PermissionResource resource;
    PermissionAction action;
    PermissionResource targetResource;
    PermissionScope scope;
    UUID orgId;
}
