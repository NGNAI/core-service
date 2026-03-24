package ai.entity.postgres.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Embeddable
public class RolePermissionIdEmbed {
    @Column(name = "role_id", nullable = false)
    UUID roleId;

    @Column(name = "permission_id", nullable = false)
    UUID permissionId;
}
