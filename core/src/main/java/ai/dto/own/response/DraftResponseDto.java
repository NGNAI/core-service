package ai.dto.own.response;

import java.util.List;
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
        "scopes",
        "formatStandard",
        "title",
        "detailedDescription",
        "latestVersionNumber",
        "latestContentPreview",
        "sessionId"
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DraftResponseDto extends AuditResponseDto {
    UUID id;
    UUID ownerId;
    UUID organizationId;
    String type;
    List<String> scopes;
    String formatStandard;
    String title;
    String detailedDescription;
    Integer latestVersionNumber;
    String latestContentPreview;
    String sessionId;
}
