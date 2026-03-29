package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@JsonPropertyOrder({
        "id",
        "topicId",
        "content",
        "type"
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageResponseDto extends AuditResponseDto {
    UUID id;
    UUID topicId;
    String content;
    @JsonRawValue
    String source;
    String type;
}
