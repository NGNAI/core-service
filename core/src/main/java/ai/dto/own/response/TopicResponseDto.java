package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

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
    UUID id;
    UUID ownerId;
    String title;
    String type;
}
