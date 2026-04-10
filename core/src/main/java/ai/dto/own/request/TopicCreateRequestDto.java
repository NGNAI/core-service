package ai.dto.own.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicCreateRequestDto {
    @NotBlank(message = "TOPIC_TITLE_CAN_NOT_BE_NULL_OR_EMPTY")
    String title;
    @NotBlank(message = "TOPIC_TYPE_CAN_NOT_BE_NULL_OR_EMPTY")
    String type;
}
