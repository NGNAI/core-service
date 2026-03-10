package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@FieldDefaults(level = AccessLevel.PROTECTED)
public class AuditResponseDto {
    String createdAt;
    Integer createdBy;
    String updatedAt;
    Integer updatedBy;
}
