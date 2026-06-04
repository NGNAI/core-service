package ai.repository;

import ai.entity.postgres.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
    Optional<UserEntity> findByUserName(String userName);
    Optional<UserEntity> findByUserNameAndSource(String userName, String source);
    boolean existsByUserName(String userName);
    
    @Query("SELECT COUNT(u) FROM UserEntity u")
    long countAllUsers();
}
