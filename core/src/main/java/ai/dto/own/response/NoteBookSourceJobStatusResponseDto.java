package ai.dto.own.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteBookSourceJobStatusResponseDto {
    UUID sourceId;
    UUID noteBookId;
    UUID jobId;
    String vectorStatus;
    String message;
}
