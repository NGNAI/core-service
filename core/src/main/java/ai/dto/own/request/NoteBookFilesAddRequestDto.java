package ai.dto.own.request;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteBookFilesAddRequestDto {
    @Schema(description = "Attachment files for FILE sources")
    MultipartFile[] files;

    @Schema(description = "Raw text content for TEXT source")
    String textContent;

    @Schema(description = "Display name for TEXT source")
    String textDisplayName;

    @Schema(description = "Existing note id for NOTE source")
    UUID noteId;

    @Schema(description = "Display name for NOTE source")
    String noteDisplayName;
}
