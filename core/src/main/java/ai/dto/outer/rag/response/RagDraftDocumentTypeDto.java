package ai.dto.outer.rag.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * DTO response cho endpoint RAG GET /draft/document-types.
 * Metadata loại tài liệu hỗ trợ cho draft (document_type).
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RagDraftDocumentTypeDto {

    @JsonProperty("value")
    String value;

    @JsonProperty("label_vi")
    String labelVi;

    @JsonProperty("label_en")
    String labelEn;

    @JsonProperty("description")
    String description;
}
