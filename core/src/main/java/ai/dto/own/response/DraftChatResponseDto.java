package ai.dto.own.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@JsonPropertyOrder({
        "draftId",
        "versionId",
        "versionNumber",
        "generatedContent"
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DraftChatResponseDto {
    UUID draftId;
    UUID versionId;
    Integer versionNumber;
    String generatedContent;
}
