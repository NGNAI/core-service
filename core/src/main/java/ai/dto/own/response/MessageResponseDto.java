package ai.dto.own.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@JsonPropertyOrder({
        "id",
        "parentId",
        "content",
        "source",
        "suggestedReplies",
        "reasoningSteps",
        "type",
        "parentType"
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageResponseDto extends AuditResponseDto {
    UUID id;
    UUID parentId;
    String content;
    @JsonRawValue
    String source;
    String feedback;
    @JsonRawValue
    String suggestedReplies;
    @JsonRawValue
    String reasoningSteps;
    String type;
    String parentType;
}
