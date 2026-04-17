package ai.enums;

public enum IngestionStatus {
    CREATED, // Mới tạo
    EXTRACTING, // Đang trích xuất
    CHUNKING, // Đang chia nhỏ
    EMBEDDING, // Đang nhúng
    STORING, // Đang lưu trữ
    COMPLETED, // Hoàn thành
    FAILED // Thất bại
}
