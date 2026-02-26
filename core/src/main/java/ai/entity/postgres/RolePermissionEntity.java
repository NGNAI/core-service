package ai.entity.postgres;

import ai.entity.postgres.embeddable.RolePermissionIdEmbed;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "role_permissions")
@Entity
public class RolePermissionEntity {
    @EmbeddedId
    RolePermissionIdEmbed id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("permissionId")
    PermissionEntity permission;

    public RolePermissionEntity(RoleEntity roleEntity, PermissionEntity permissionEntity){
        id = new RolePermissionIdEmbed(roleEntity.getId(), permissionEntity.getId());

        role = roleEntity;
        permission = permissionEntity;
    }
}
