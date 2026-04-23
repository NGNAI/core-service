package ai.dto.own.response;

import ai.enums.SystemEventSource;
import ai.enums.SystemEventType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemSseEventResponseDto {
    SystemEventType type;
    SystemEventSource source;
    String sourceId;
    Object data;
    long timestamp;
}