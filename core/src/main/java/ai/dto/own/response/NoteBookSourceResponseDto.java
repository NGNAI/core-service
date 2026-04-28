package ai.dto.own.response;

import ai.entity.postgres.NoteBookSourceEntity;
import ai.enums.DataIngestionDeleteStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Response DTO cho một nguồn dữ liệu thuộc notebook.
 */
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteBookSourceResponseDto extends AuditResponseDto {
    UUID id;
    UUID noteBookId;
    UUID noteId;
    NoteBookSourceEntity.SourceType sourceType;
    String displayName;
    String rawContent;
    String filePath;
    String summary;
    String metadata;
    NoteBookSourceEntity.VectorStatus vectorStatus;
    UUID jobId;
    UUID ownerId;
    UUID organizationId;
    DataIngestionDeleteStatus deleteStatus;
}
