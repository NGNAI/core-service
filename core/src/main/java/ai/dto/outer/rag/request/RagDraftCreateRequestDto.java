package ai.dto.outer.rag.request;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagDraftCreateRequestDto {

    @JsonProperty("user_request")
    String user_request;

    @JsonProperty("document_type")
    String document_type;

    @JsonProperty("context")
    String context;

    @JsonProperty("user_id")
    UUID userId;

    @JsonProperty("organization_id")
    UUID organizationId;

    @JsonProperty("stream")
    boolean stream;
}
