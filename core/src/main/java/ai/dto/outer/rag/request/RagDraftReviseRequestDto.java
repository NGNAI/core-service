package ai.dto.outer.rag.request;

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
public class RagDraftReviseRequestDto {

    @JsonProperty("session_id")
    String session_id;

    @JsonProperty("feedback")
    String feedback;

    @JsonProperty("stream")
    boolean stream = true;

    @JsonProperty("max_iterations")
    Integer max_iterations;
}
