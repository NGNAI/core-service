package ai.dto.own.response.dashboard;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.List;

@JsonPropertyOrder({
        "total",
        "items"
})
@Data
@FieldDefaults(level = AccessLevel.PROTECTED)
public class RecentActivitiesDto implements Serializable {
    Long total;
    List<ai.dto.own.response.AuditLogResponseDto> items;
}
