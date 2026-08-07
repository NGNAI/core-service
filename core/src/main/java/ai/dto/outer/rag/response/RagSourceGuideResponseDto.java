package ai.dto.outer.rag.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO chung cho các API source-guide (NotebookLM) trên RAG service:
 * <ul>
 *   <li>POST /notebook/v2/source-guide (trigger) — response ban đầu thường status=processing</li>
 *   <li>GET /notebook/v2/source-guide/{file_id} (lấy kết quả)</li>
 *   <li>Webhook callback từ RAG service</li>
 * </ul>
 *
 * <p>status có thể là: completed / failed / processing / not_found</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagSourceGuideResponseDto {
    /** ID của file/source (ở đây là UUID của NoteBookSourceEntity) */
    @JsonProperty("file_id")
    String fileId;

    /** completed / failed / processing / not_found */
    String status;

    /** Nội dung tóm tắt, chỉ có khi status = completed */
    String summary;

    /** Danh sách file nguồn được dùng để tóm tắt */
    @JsonProperty("source_files")
    List<String> sourceFiles;

    /** Thông báo lỗi, chỉ có khi status = failed */
    String error;
}
