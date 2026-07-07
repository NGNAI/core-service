package ai.repository;

import ai.entity.postgres.SystemSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSettingEntity, UUID> {

    Optional<SystemSettingEntity> findByKey(String key);

    List<SystemSettingEntity> findByGroupNameOrderByDisplayOrderAsc(String groupName);

    List<SystemSettingEntity> findByIsActiveTrueOrderByGroupNameAscDisplayOrderAsc();

    List<SystemSettingEntity> findByIsPublicTrueAndIsActiveTrueOrderByGroupNameAscDisplayOrderAsc();

    List<SystemSettingEntity> findByGroupNameAndIsActiveTrueOrderByDisplayOrderAsc(String groupName);

    boolean existsByKey(String key);

    void deleteByKey(String key);
}
