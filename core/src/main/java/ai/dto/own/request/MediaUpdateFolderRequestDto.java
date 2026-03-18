package ai.dto.own.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaUpdateFolderRequestDto {
    String name;
    UUID parentId;
    Boolean moveToRoot;
}
