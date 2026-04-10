package ai.dto.own.response;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttachmentResponseDto extends AuditResponseDto {
    UUID id;
    String name;
    String minioPath;
    Long fileSize;
    String contentType;
    UUID topicId;
    UUID messageId;
    UUID ownerId;
    UUID orgId;
}
