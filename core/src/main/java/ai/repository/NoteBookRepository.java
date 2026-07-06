package ai.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.NoteBookEntity;

@Repository
public interface NoteBookRepository extends JpaRepository<NoteBookEntity, UUID>, JpaSpecificationExecutor<NoteBookEntity> {
    @Query("""
        SELECT COUNT(t) > 0
        FROM NoteBookEntity t
        WHERE t.id = :id
          AND t.owner.id = :ownerId
    """)
    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
    
    @Query("SELECT COUNT(n) FROM NoteBookEntity n")
    long countAllNoteBooks();
    
    @Query("SELECT COUNT(n) FROM NoteBookEntity n WHERE n.organization.id = :orgId")
    long countAllNoteBooksByOrgId(UUID orgId);

    // Count notebooks by updated date (day precision)
    @Query(value = "SELECT COUNT(n) FROM notebook n WHERE DATE(n.updated_at) = :date", nativeQuery = true)
    long countNoteBooksByDate(java.time.LocalDate date);

    // Count notebooks by updated date and organization
    @Query(value = "SELECT COUNT(n) FROM notebook n WHERE DATE(n.updated_at) = :date AND n.organization_id = :orgId", nativeQuery = true)
    long countNoteBooksByDateAndOrgId(java.time.LocalDate date, UUID orgId);
}
