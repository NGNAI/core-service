package ai.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.DraftVersionEntity;

@Repository
public interface DraftVersionRepository extends JpaRepository<DraftVersionEntity, UUID> {
    Page<DraftVersionEntity> findByDraft_IdOrderByVersionNumberDesc(UUID draftId, Pageable pageable);

    Optional<DraftVersionEntity> findFirstByDraft_IdOrderByVersionNumberDesc(UUID draftId);

    Optional<DraftVersionEntity> findByIdAndDraft_Id(UUID id, UUID draftId);

    long countByDraft_Id(UUID draftId);
}
