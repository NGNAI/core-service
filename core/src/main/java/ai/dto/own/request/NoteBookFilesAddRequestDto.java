package ai.dto.own.request;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteBookFilesAddRequestDto {
    @NotNull
    @Schema(description = "Optional attachment files for the conversation notebook")
    MultipartFile[] files;
}
