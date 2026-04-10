package ai.dto.own.request;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationAttachmentUploadRequestDto {
    @Schema(description = "Attachment file for conversation topic")
    @NotNull(message = "ATTACHMENT_FILE_REQUIRED")
    MultipartFile file;
}
