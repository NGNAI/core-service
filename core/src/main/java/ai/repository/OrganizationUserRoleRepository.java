package ai.repository;

import ai.entity.postgres.OrganizationUserRoleEntity;
import ai.entity.postgres.embeddable.OrganizationUserRoleIdEmbed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
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

    @Query("""
        SELECT COUNT(DISTINCT our.user.id)
        FROM OrganizationUserRoleEntity our
        WHERE our.organization.id IN :orgIds
        """)
    long countUsersByOrgIds(@Param("orgIds") Collection<UUID> orgIds);

    @Query("""
        SELECT COUNT(DISTINCT our.user.id)
        FROM OrganizationUserRoleEntity our
        JOIN our.user u
        WHERE our.organization.id IN :orgIds AND u.active = :active
        """)
    long countUsersByOrgIdsAndActive(@Param("orgIds") Collection<UUID> orgIds, @Param("active") boolean active);

    @Query("""
        SELECT our.organization.id as orgId,
               COUNT(DISTINCT our.user.id) as total,
               COUNT(DISTINCT CASE WHEN u.active = true THEN our.user.id END) as active,
               COUNT(DISTINCT CASE WHEN u.active = false THEN our.user.id END) as inactive
        FROM OrganizationUserRoleEntity our
        JOIN our.user u
        WHERE our.organization.id IN :orgIds
        GROUP BY our.organization.id
        """)
    List<Object[]> countUsersGroupByOrg(@Param("orgIds") Collection<UUID> orgIds);

    @Query("""
        SELECT our.role.id as roleId,
               our.role.name as roleName,
               COUNT(DISTINCT our.user.id) as total
        FROM OrganizationUserRoleEntity our
        WHERE our.organization.id IN :orgIds
        GROUP BY our.role.id, our.role.name
        """)
    List<Object[]> countUsersGroupByRole(@Param("orgIds") Collection<UUID> orgIds);
}
