package ai.repository;

import ai.entity.postgres.DraftSourceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DraftSourceRepository extends JpaRepository<DraftSourceEntity, UUID> {

    Page<DraftSourceEntity> findByDraftId(UUID draftId, Pageable pageable);

    List<DraftSourceEntity> findByDraftId(UUID draftId);

    boolean existsByDraftIdAndDisplayNameAndSourceType(UUID draftId, String displayName, DraftSourceEntity.SourceType sourceType);

    Optional<DraftSourceEntity> findByDraftIdAndId(UUID draftId, UUID sourceId);

    @Query("SELECT COUNT(ds) FROM DraftSourceEntity ds WHERE ds.draft.id = :draftId")
    long countByDraftId(UUID draftId);
}
