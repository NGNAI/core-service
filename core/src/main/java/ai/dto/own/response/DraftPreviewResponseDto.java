package ai.dto.own.response;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DraftPreviewResponseDto {
    UUID draftId;
    String type;
    String title;
    String presentationStyle;
    String language;
    String generatedContent;
}
