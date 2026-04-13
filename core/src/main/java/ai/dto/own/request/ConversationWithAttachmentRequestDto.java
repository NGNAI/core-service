package ai.dto.own.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationWithAttachmentRequestDto {
    @NotBlank(message = "MESSAGE_CAN_NOT_BE_NULL_OR_EMPTY")
    String message;

    @Schema(description = "Optional attachment files for the conversation topic")
    MultipartFile[] files;
}
