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
        JOIN our.user u
        WHERE our.organization.id = :orgId
        AND (:keyword IS NULL OR :keyword = '' OR
             LOWER(FUNCTION('unaccent', u.userName)) LIKE LOWER(CONCAT('%', FUNCTION('unaccent', :keyword), '%')) OR
             LOWER(FUNCTION('unaccent', u.firstName)) LIKE LOWER(CONCAT('%', FUNCTION('unaccent', :keyword), '%')) OR
             LOWER(FUNCTION('unaccent', u.lastName)) LIKE LOWER(CONCAT('%', FUNCTION('unaccent', :keyword), '%')) OR
             LOWER(FUNCTION('unaccent', u.email)) LIKE LOWER(CONCAT('%', FUNCTION('unaccent', :keyword), '%')) OR
             LOWER(FUNCTION('unaccent', u.phoneNumber)) LIKE LOWER(CONCAT('%', FUNCTION('unaccent', :keyword), '%')))
        AND (:source IS NULL OR :source = '' OR u.source = :source)
        """)
    long countDistinctUsersByOrgId(@Param("orgId") UUID orgId,
                                   @Param("keyword") String keyword,
                                   @Param("source") String source);

    @Query("""
        SELECT COUNT(DISTINCT our.user.id)
        FROM OrganizationUserRoleEntity our
        JOIN our.user u
        WHERE our.organization.id = :orgId
        AND our.user.id NOT IN (
            SELECT our2.user.id
            FROM OrganizationUserRoleEntity our2
            WHERE our2.organization.id = :orgId AND our2.role.id = :roleId
        )
        AND (:keyword IS NULL OR :keyword = '' OR
             LOWER(FUNCTION('unaccent', u.userName)) LIKE LOWER(CONCAT('%', FUNCTION('unaccent', :keyword), '%')) OR
             LOWER(FUNCTION('unaccent', u.firstName)) LIKE LOWER(CONCAT('%', FUNCTION('unaccent', :keyword), '%')) OR
             LOWER(FUNCTION('unaccent', u.lastName)) LIKE LOWER(CONCAT('%', FUNCTION('unaccent', :keyword), '%')) OR
             LOWER(FUNCTION('unaccent', u.email)) LIKE LOWER(CONCAT('%', FUNCTION('unaccent', :keyword), '%')) OR
             LOWER(FUNCTION('unaccent', u.phoneNumber)) LIKE LOWER(CONCAT('%', FUNCTION('unaccent', :keyword), '%')))
        AND (:source IS NULL OR :source = '' OR u.source = :source)
        """)
    long countDistinctUsersByOrgIdNotInRole(@Param("orgId") UUID orgId,
                                            @Param("roleId") UUID roleId,
                                            @Param("keyword") String keyword,
                                            @Param("source") String source);

    @Query("""
        SELECT our
        FROM OrganizationUserRoleEntity our
        JOIN FETCH our.user
        JOIN FETCH our.role
        WHERE our.organization.id = :orgId
        AND our.user.id IN :userIds
        """)
    List<OrganizationUserRoleEntity> findByOrganizationIdAndUserIdInWithFetch(@Param("orgId") UUID orgId, @Param("userIds") Collection<UUID> userIds);

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
