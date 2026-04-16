package ai.repository;

import ai.entity.postgres.NoteBookEntity;
import ai.entity.postgres.TopicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NoteBookRepository extends JpaRepository<NoteBookEntity, UUID>, JpaSpecificationExecutor<NoteBookEntity> {
    @Query("""
        SELECT COUNT(t) > 0
        FROM NoteBookEntity t
        WHERE t.id = :id
          AND t.owner.id = :ownerId
    """)
    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}
