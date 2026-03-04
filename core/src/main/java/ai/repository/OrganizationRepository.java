package ai.repository;

import ai.entity.postgres.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Integer> {
    List<OrganizationEntity> findByParentId(int parentId);
    List<OrganizationEntity> findByParentIsNull();
    int countByParentId(int parentId);
}
