package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Cấu hình giới hạn upload cho một loại (Topic/Notebook/Draft/Data-Ingestion).
 * Giá trị = 0 nghĩa là không giới hạn (unlimited).
 */
@JsonPropertyOrder({
        "type",
        "maxFileCount",
        "maxFileSizeMb",
        "maxTotalSources",
        "allowedFileTypes"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UploadConfigResponseDto {
    String type;
    Integer maxFileCount;
    Integer maxFileSizeMb;
    Integer maxTotalSources;
    String allowedFileTypes;
}
