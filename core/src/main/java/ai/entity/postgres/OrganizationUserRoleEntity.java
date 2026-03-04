package ai.entity.postgres;

import ai.entity.postgres.embeddable.OrganizationUserRoleIdEmbed;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "organization_user_role")
@Entity
public class OrganizationUserRoleEntity {
    @EmbeddedId
    OrganizationUserRoleIdEmbed id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("organizationId")
    OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    RoleEntity role;

    public OrganizationUserRoleEntity(OrganizationEntity orgEntity, UserEntity userEntity, RoleEntity roleEntity){
        id = new OrganizationUserRoleIdEmbed(orgEntity.getId(), userEntity.getId(), roleEntity.getId());

        organization = orgEntity;
        user = userEntity;
        role = roleEntity;
    }
}
