package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

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
    int id;
    int topicId;
    String content;
    String type;
}
