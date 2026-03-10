package ai.repository;

import ai.entity.postgres.RoleEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Integer> {
    boolean existsByName(String name);

    @Modifying
    @Transactional
    @Query("""
        UPDATE RoleEntity r SET r.defaultAssign = false
        """)
    void deActiveAllDefaultAssign();

    @EntityGraph(attributePaths = {
            "rolePermissions",
            "rolePermissions.permission"
    })
    Page<RoleEntity> findAll(Specification<RoleEntity> spec, Pageable pageable);

    @Query("""
       SELECT r
       FROM RoleEntity r
       LEFT JOIN FETCH r.rolePermissions rp
       LEFT JOIN FETCH rp.permission
       """)
    List<RoleEntity> findAllWithPermissions();

    @Query("""
       SELECT r
       FROM RoleEntity r
       LEFT JOIN FETCH r.rolePermissions rp
       LEFT JOIN FETCH rp.permission
       WHERE r.id = :roleId
       """)
    Optional<RoleEntity> findByIdWithPermissions(int roleId);

    @Query("""
        SELECT r
        FROM RoleEntity r
        WHERE r.defaultAssign = true
        """)
    Optional<RoleEntity> findByDefaultAssign();
}
