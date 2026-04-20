package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@JsonPropertyOrder({
        "id",
        "ownerId",
        "title",
        "instruction",
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteBookResponseDto extends AuditResponseDto {
    UUID id;
    UUID ownerId;
    String title;
    String instruction;
}
