package ai.dto.own.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicSourcesAddRequestDto {
    @Schema(description = "Attachment files for FILE sources")
    MultipartFile[] files;
}
