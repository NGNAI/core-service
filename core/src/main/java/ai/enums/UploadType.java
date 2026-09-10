package ai.enums;

/**
 * Loại luồng upload file. Dùng để phân biệt cấu hình giới hạn upload
 * (số lượng file, dung lượng mỗi file, loại file được phép, tổng source) cho từng loại.
 */
public enum UploadType {
    TOPIC,
    NOTEBOOK,
    DRAFT,
    DATA_INGESTION
}
