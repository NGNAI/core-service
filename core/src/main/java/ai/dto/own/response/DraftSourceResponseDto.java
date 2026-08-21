package ai.dto.own.response;

import ai.entity.postgres.DraftSourceEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DraftSourceResponseDto extends AuditResponseDto {
    UUID id;
    UUID draftId;
    DraftSourceEntity.SourceType sourceType;
    String displayName;
    String rawContent;
    String filePath;
    String summary;
    String metadata;
    DraftSourceEntity.VectorStatus vectorStatus;
}
