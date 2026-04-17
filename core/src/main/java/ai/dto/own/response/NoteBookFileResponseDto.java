package ai.dto.own.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Response DTO cho một bản ghi notebook_file — liên kết giữa NoteBook và một DataIngestion kèm metadata ngữ cảnh.
 */
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteBookFileResponseDto extends AuditResponseDto {
    UUID id;
    UUID noteBookId;
    String summary;
    String metadata;
    DataIngestionResponseDto dataIngestion;
}
