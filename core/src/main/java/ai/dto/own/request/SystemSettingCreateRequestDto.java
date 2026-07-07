package ai.dto.own.request;

import jakarta.validation.constraints.NotBlank;
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
public class SystemSettingCreateRequestDto {

    @NotBlank(message = "SETTING_KEY_CAN_NOT_BE_NULL_OR_EMPTY")
    String key;

    String value;

    String description;

    @NotBlank(message = "SETTING_TYPE_CAN_NOT_BE_NULL_OR_EMPTY")
    @Builder.Default
    String type = "STRING";

    @NotBlank(message = "SETTING_GROUP_CAN_NOT_BE_NULL_OR_EMPTY")
    @Builder.Default
    String groupName = "GENERAL";

    @Builder.Default
    Boolean isPublic = false;

    @Builder.Default
    Boolean isActive = true;

    @Builder.Default
    Integer displayOrder = 0;
}
