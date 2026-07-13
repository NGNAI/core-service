package ai.repository;

import ai.entity.postgres.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID>, JpaSpecificationExecutor<OrganizationEntity> {
    List<OrganizationEntity> findByParentId(UUID parentId);
    int countByParentId(UUID parentId);
    
    @Query("SELECT COUNT(o) FROM OrganizationEntity o")
    long countAllOrganizations();

    @Query("""
        SELECT o.id FROM OrganizationEntity o
        WHERE o.path LIKE :pathPrefix OR o.id = :orgId
        """)
    List<UUID> findDescendantOrgIds(@Param("orgId") UUID orgId, @Param("pathPrefix") String pathPrefix);
}
