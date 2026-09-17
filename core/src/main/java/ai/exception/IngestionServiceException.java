package ai.exception;

import ai.enums.ApiResponseStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Ngoại lệ khi gọi ingestion service (RAG) gặp lỗi, mang kèm errorBody
 * (body response gốc dạng string) để lưu lại nguyên nhân lỗi cụ thể
 * (vd: {"detail": "File too large ..."}) nhằm hỗ trợ debug.
 *
 * <p>Ngoài ra còn mang {@code httpStatusCode} (nếu có) để bên gọi phân biệt được
 * lỗi nghiệp vụ vĩnh viễn (vd 404 job không còn tồn tại trên ingestion service)
 * với lỗi tạm thời (mất kết nối, timeout, 5xx) — từ đó quyết định có nên tiếp tục
 * poll trạng thái hay dừng lại và đánh dấu thất bại (tránh vòng lặp vô hạn).</p>
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IngestionServiceException extends AppException {
    String errorBody;

    /** HTTP status code trả về từ ingestion service, null nếu lỗi không phải do response HTTP. */
    Integer httpStatusCode;

    public IngestionServiceException(ApiResponseStatus apiResponseStatus, String errorBody) {
        this(apiResponseStatus, errorBody, null);
    }

    public IngestionServiceException(ApiResponseStatus apiResponseStatus, String errorBody, Integer httpStatusCode) {
        super(apiResponseStatus);
        this.errorBody = errorBody;
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * Kiểm tra lỗi có phải do ingestion service trả về 404 (không tìm thấy job/file).
     * Đây là lỗi <b>vĩnh viễn</b>: poll lại jobId đó sẽ luôn thất bại, nên bên gọi
     * cần dừng hẳn thay vì retry vô hạn.
     */
    public boolean isNotFound() {
        return httpStatusCode != null && httpStatusCode == 404;
    }
}
