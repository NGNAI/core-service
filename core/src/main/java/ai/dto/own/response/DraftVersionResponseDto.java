package ai.dto.own.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@JsonPropertyOrder({
        "id",
        "draftId",
        "versionNumber",
        "detailedDescription",
        "changeRequest",
        "generatedContent",
        "createdAt",
        "createdBy",
        "updatedAt",
        "updatedBy"
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DraftVersionResponseDto extends AuditResponseDto {
    UUID id;
    UUID draftId;
    Integer versionNumber;
    String detailedDescription;
    String changeRequest;
    String generatedContent;
}
