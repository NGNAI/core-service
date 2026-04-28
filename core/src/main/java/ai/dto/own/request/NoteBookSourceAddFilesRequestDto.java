package ai.dto.own.request;

import ai.constant.InputValidateKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteBookSourceAddFilesRequestDto {
    @NotNull(message = InputValidateKey.DATA_INGESTION_FILE_REQUIRED)
    @Schema(description = "Attachment files for FILE sources")
    MultipartFile[] files;
}