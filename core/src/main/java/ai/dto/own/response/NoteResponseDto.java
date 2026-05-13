package ai.dto.own.response;

import ai.enums.NoteSourceBy;
import ai.enums.NoteSourceType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteResponseDto extends AuditResponseDto {
    UUID id;
    String title;
    String content;
    NoteSourceType sourceType;
    NoteSourceBy sourceBy;
    UUID topicId;
    UUID noteBookId;
    UUID ownerId;
    UUID orgId;
}
