package ai.dto.own.response;

import java.util.UUID;

import ai.enums.DataSource;
import ai.enums.DataScope;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataIngestionResponseDto extends AuditResponseDto {
    UUID id;
    String name;
    boolean folder;
    String contentType;
    Long fileSize;
    String minioPath;
    UUID parentId;
    UUID ownerId;
    UUID orgId;
    DataScope accessLevel;
    DataSource fromSource;
    UUID jobId;
    String ingestionStatus;
    String deleteStatus;
}
