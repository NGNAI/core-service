package ai.repository;

import ai.entity.postgres.OrganizationUserRoleEntity;
import ai.entity.postgres.embeddable.OrganizationUserRoleIdEmbed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationUserRoleRepository extends JpaRepository<OrganizationUserRoleEntity, OrganizationUserRoleIdEmbed>, JpaSpecificationExecutor<OrganizationUserRoleEntity> {
    @Query("""
        SELECT our
        FROM OrganizationUserRoleEntity our
        JOIN FETCH our.user
        JOIN FETCH our.role
        WHERE our.organization.id = :organizationId
        """)
    List<OrganizationUserRoleEntity> findUserRoleByOrgId(UUID organizationId);

    @Query("""
        SELECT COUNT(our)
        FROM OrganizationUserRoleEntity our
        WHERE our.organization.id = :organizationId
        """)
    int countUserRoleByOrgId(UUID organizationId);

    List<OrganizationUserRoleEntity> findByOrganizationIdAndUserIdIn(UUID orgId, Collection<UUID> userIds);

    List<OrganizationUserRoleEntity> findByOrganizationIdAndRoleId(UUID orgId, UUID roleId);

    List<OrganizationUserRoleEntity> findByOrganizationIdAndRoleIdNot(UUID orgId, UUID roleId);

    @Query("""
        SELECT our
        FROM OrganizationUserRoleEntity our
        JOIN fetch our.organization
        JOIN fetch our.role r
        LEFT JOIN fetch r.rolePermissions rp
        LEFT JOIN fetch rp.permission
        WHERE our.user.id = :userId
        """)
    List<OrganizationUserRoleEntity> findByUserWithPermission(UUID userId);

    @Query("""
        SELECT our
        FROM OrganizationUserRoleEntity our
        JOIN fetch our.organization
        JOIN fetch our.role r
        LEFT JOIN fetch r.rolePermissions rp
        LEFT JOIN fetch rp.permission
        WHERE our.user.id = :userId AND our.organization.id = :orgId
        """)
    List<OrganizationUserRoleEntity> findByUserAndOrgWithPermission(UUID userId, UUID orgId);
}
