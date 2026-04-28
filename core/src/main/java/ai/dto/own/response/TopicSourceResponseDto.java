package ai.dto.own.response;

import ai.entity.postgres.TopicSourceEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicSourceResponseDto extends AuditResponseDto {
    UUID id;
    UUID topicId;
    TopicSourceEntity.SourceType sourceType;
    String displayName;
    String rawContent;
    String filePath;
    String summary;
    String metadata;
    TopicSourceEntity.VectorStatus vectorStatus;
}
