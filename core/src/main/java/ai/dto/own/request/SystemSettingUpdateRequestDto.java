package ai.dto.own.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemSettingUpdateRequestDto {

    String key;

    String value;

    String description;

    String type;

    String groupName;

    Boolean isPublic;

    Boolean isActive;

    Integer displayOrder;
}
