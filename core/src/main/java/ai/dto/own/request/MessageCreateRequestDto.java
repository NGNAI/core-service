package ai.dto.own.request;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageCreateRequestDto {
    String content;
    String type;
    int topicId;
}
