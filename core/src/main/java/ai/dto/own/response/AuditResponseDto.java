package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditResponseDto {
    String createdAt;
    String createdBy;
    String updatedAt;
    String updatedBy;
}
