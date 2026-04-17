package ai.dto.own.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Response DTO cho một bản ghi topic_file — liên kết giữa Topic và một DataIngestion kèm metadata ngữ cảnh.
 */
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicFileResponseDto extends AuditResponseDto {
    UUID id;
    UUID topicId;
    UUID messageId;
    String summary;
    String metadata;
    DataIngestionResponseDto dataIngestion;
}
