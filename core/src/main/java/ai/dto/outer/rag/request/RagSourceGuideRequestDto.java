package ai.dto.outer.rag.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO cho API trigger source-guide (NotebookLM) trên RAG service.
 * POST /notebook/v2/source-guide — dùng để sinh summary cho một file trong notebook.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagSourceGuideRequestDto {
    /** ID của file/source (ở đây là UUID của NoteBookSourceEntity) */
    @JsonProperty("file_id")
    String fileId;

    @JsonProperty("notebook_id")
    String notebookId;

    @JsonProperty("organization_id")
    String organizationId;

    /** Phạm vi xử lý, mặc định ["global"] */
    @JsonProperty("scopes")
    List<String> scopes;

    @JsonProperty("user_id")
    String userId;

    /** true = ép sinh lại summary kể cả khi đã có */
    @JsonProperty("force_regenerate")
    boolean forceRegenerate;

    /** URL để RAG service callback kết quả source-guide về core-service */
    @JsonProperty("callback_url")
    String callbackUrl;
}
