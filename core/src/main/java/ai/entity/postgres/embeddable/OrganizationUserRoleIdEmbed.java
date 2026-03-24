package ai.entity.postgres.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Embeddable
public class OrganizationUserRoleIdEmbed {
    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "role_id", nullable = false)
    UUID roleId;
}
