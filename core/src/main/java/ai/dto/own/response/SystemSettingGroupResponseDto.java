package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@JsonPropertyOrder({
        "groupName",
        "settings"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemSettingGroupResponseDto {
    String groupName;
    List<SystemSettingResponseDto> settings;
}
