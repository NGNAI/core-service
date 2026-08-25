package ai.dto.own.response;

import ai.enums.FeedbackStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackResponseDto extends AuditResponseDto {
    UUID id;
    String subject;
    String content;
    Boolean isPrivate;
    FeedbackStatus status;
    String responseContent;
    Instant responseDate;
    
    // Sender info
    String senderName;
    UUID senderId;
    String senderOrgName;
    UUID senderOrgId;
    
    // Responder info (admin)
    String responderName;
    UUID responderId;
    String responderOrgName;
    UUID responderOrgId;
}
