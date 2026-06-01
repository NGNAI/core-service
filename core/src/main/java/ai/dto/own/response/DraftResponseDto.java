package ai.dto.own.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@JsonPropertyOrder({
        "id",
        "ownerId",
        "organizationId",
        "type",
        "title",
        "detailedDescription",
        "presentationStyle",
        "language",
        "tone",
        "targetAudience",
        "outputLength",
        "formatInstruction",
        "additionalInstruction",
        "latestVersionNumber",
        "latestContentPreview"
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DraftResponseDto extends AuditResponseDto {
    UUID id;
    UUID ownerId;
    UUID organizationId;
    String type;
    String title;
    String detailedDescription;
    String presentationStyle;
    String language;
    String tone;
    String targetAudience;
    String outputLength;
    String formatInstruction;
    String additionalInstruction;
    Integer latestVersionNumber;
    String latestContentPreview;
}
