package ai.repository;

import ai.entity.postgres.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, Integer>, JpaSpecificationExecutor<PermissionEntity> {
    boolean existsByName(String name);
}
