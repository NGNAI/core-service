package ai.dto.own.request;

import ai.enums.FeedbackStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackStatusUpdateRequestDto {
    FeedbackStatus status;
}
