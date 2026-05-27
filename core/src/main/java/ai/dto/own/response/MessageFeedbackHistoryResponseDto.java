package ai.dto.own.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@JsonPropertyOrder({
        "id",
        "messageId",
        "beforeFeedback",
        "afterFeedback",
        "createdAt",
        "createdBy",
        "updatedAt",
        "updatedBy"
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageFeedbackHistoryResponseDto extends AuditResponseDto {
    UUID id;
    UUID messageId;
    String beforeFeedback;
    String afterFeedback;
}
