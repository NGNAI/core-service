package ai.repository;

import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.OrganizationUserRoleEntity;
import ai.entity.postgres.UserEntity;
import ai.entity.postgres.embeddable.RolePermissionIdEmbed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrganizationUserRoleRepository extends JpaRepository<OrganizationUserRoleEntity, RolePermissionIdEmbed> {
    @Query("""
        SELECT our
        FROM OrganizationUserRoleEntity our
        JOIN FETCH our.user
        JOIN FETCH our.role
        WHERE our.organization.id = :organizationId
        """)
    List<OrganizationUserRoleEntity> findUserRoleByOrgId(int organizationId);

    @Query("""
        SELECT COUNT(our)
        FROM OrganizationUserRoleEntity our
        WHERE our.organization.id = :organizationId
        """)
    int countUserRoleByOrgId(int organizationId);

    List<OrganizationUserRoleEntity> findByOrganizationIdAndUserIdIn(int orgId, Collection<Integer> userIds);

    List<OrganizationUserRoleEntity> findByOrganizationIdAndRoleId(int orgId, int roleId);

    @Query("""
        SELECT our
        FROM OrganizationUserRoleEntity our
        JOIN fetch our.organization
        JOIN fetch our.role r
        LEFT JOIN fetch r.rolePermissions rp
        LEFT JOIN fetch rp.permission
        WHERE our.user.id = :userId
        """)
    List<OrganizationUserRoleEntity> findByUserWithPermission(int userId);

    @Query("""
        SELECT u
        FROM UserEntity u
        WHERE NOT EXISTS (
            SELECT 1
            FROM OrganizationUserRoleEntity our
            WHERE our.organization.id = :orgId
                AND our.user.id = u.id
            )
        """)
    List<UserEntity> findUsersNotInOrg(int orgId);
}
