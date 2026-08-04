package ai.exception;

import ai.enums.ApiResponseStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Ngoại lệ khi gọi ingestion service (RAG) gặp lỗi, mang kèm errorBody
 * (body response gốc dạng string) để lưu lại nguyên nhân lỗi cụ thể
 * (vd: {"detail": "File too large ..."}) nhằm hỗ trợ debug.
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IngestionServiceException extends AppException {
    String errorBody;

    public IngestionServiceException(ApiResponseStatus apiResponseStatus, String errorBody) {
        super(apiResponseStatus);
        this.errorBody = errorBody;
    }
}
