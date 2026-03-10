package ai.repository;

import ai.entity.postgres.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Integer>, JpaSpecificationExecutor<OrganizationEntity> {
    List<OrganizationEntity> findByParentId(int parentId);
    int countByParentId(int parentId);
}
