package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@JsonPropertyOrder({
        "createdAt",
        "createdBy",
        "updatedAt",
        "updatedBy"
})
@Data
@FieldDefaults(level = AccessLevel.PROTECTED)
public class AuditResponseDto {
    String createdAt;
    Integer createdBy;
    String updatedAt;
    Integer updatedBy;
}
