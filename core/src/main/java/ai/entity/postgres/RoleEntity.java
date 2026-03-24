package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "role")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "description")
    String description;

    @Column(name = "default_assign")
    boolean defaultAssign;

    @Embedded
    AuditEmbed audit= new AuditEmbed();

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<RolePermissionEntity> rolePermissions = new HashSet<>();

    @OneToMany(mappedBy = "role")
    Set<OrganizationUserRoleEntity> orgUsersRole;

    public void addPermission(PermissionEntity permissionEntity){
        RolePermissionEntity rolePermissionEntity = new RolePermissionEntity(this,permissionEntity);

        rolePermissions.add(rolePermissionEntity);
    }
}
