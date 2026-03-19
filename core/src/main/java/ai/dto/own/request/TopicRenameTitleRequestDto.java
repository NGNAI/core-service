package ai.dto.own.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicRenameTitleRequestDto {
    @NotBlank(message = "TOPIC_TITLE_CAN_NOT_BE_NULL")
    String title;
}
