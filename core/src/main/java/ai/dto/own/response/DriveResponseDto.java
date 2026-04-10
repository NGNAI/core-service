package ai.dto.own.response;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriveResponseDto extends AuditResponseDto {
    UUID id;
    String name;
    boolean folder;
    String contentType;
    Long fileSize;
    String minioPath;
    UUID parentId;
    UUID ownerId;
    UUID orgId;
    String deleteStatus;
}
