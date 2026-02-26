package ai.repository;

import ai.entity.postgres.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Integer> {
    boolean existsByName(String name);

    @Query("""
       select r
       from RoleEntity r
       left join fetch r.rolePermissions rp
       left join fetch rp.permission
       """)
    List<RoleEntity> findAllWithPermissions();
}
