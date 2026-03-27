package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.UUID;

@JsonPropertyOrder({
        "createdAt",
        "createdBy",
        "updatedAt",
        "updatedBy"
})
@Data
@FieldDefaults(level = AccessLevel.PROTECTED)
public class AuditResponseDto implements Serializable {
    String createdAt;
    UUID createdBy;
    String updatedAt;
    UUID updatedBy;
}
