package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@JsonPropertyOrder({
        "id",
        "ownerId",
        "title",
        "type"
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicResponseDto extends AuditResponseDto {
    int id;
    int ownerId;
    String title;
    String type;
}
