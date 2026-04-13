package ai.dto.own.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationRequestDto {
    @NotBlank(message = "MESSAGE_CAN_NOT_BE_NULL_OR_EMPTY")
    String message;

    @NotEmpty(message = "RAG_SCOPE_CAN_NOT_BE_NULL_OR_EMPTY")
    Set<String> scopes;

    @Schema(description = "Optional attachment files for the conversation topic")
    MultipartFile[] files;
}
