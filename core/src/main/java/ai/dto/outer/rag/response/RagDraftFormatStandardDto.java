package ai.dto.outer.rag.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * DTO response cho endpoint RAG GET /draft/format-standards.
 * Metadata chuẩn định dạng văn bản (format_standard) cho draft.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RagDraftFormatStandardDto {

    @JsonProperty("id")
    String id;

    @JsonProperty("label_vi")
    String labelVi;

    @JsonProperty("label_en")
    String labelEn;

    @JsonProperty("description")
    String description;
}
