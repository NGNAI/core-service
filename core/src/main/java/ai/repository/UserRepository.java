package ai.repository;

import ai.entity.postgres.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer>, JpaSpecificationExecutor<UserEntity> {
    Optional<UserEntity> findByUserName(String userName);
    boolean existsByUserName(String userName);
}
