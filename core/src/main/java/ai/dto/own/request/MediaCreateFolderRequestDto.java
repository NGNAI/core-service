package ai.dto.own.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaCreateFolderRequestDto {
    @NotBlank(message = "MEDIA_NAME_REQUIRED")
    String name;

    UUID parentId;
}
