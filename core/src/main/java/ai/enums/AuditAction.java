package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum AuditAction {
    LOGIN("LOGIN", "Đăng nhập"),
    LOGOUT("LOGOUT", "Đăng xuất"),
    SELECT_ORG("SELECT_ORG", "Chọn tổ chức"),

    CREATE("CREATE", "Tạo mới"),
    UPDATE("UPDATE", "Cập nhật"),
    DELETE("DELETE", "Xoá"),
    READ("READ", "Xem"),
    ASSIGN("ASSIGN", "Phân bổ"),
    REMOVE("REMOVE", "Gỡ bỏ"),
    PREVIEW("PREVIEW", "Xem trước"),
    SAVE_VERSION("SAVE_VERSION", "Lưu phiên bản"),
    ROLLBACK("ROLLBACK", "Khôi phục phiên bản"),
    UPLOAD("UPLOAD", "Tải lên"),
    DOWNLOAD("DOWNLOAD", "Tải xuống"),
    INGEST("INGEST", "Nạp dữ liệu"),
    CHAT("CHAT", "Hội thoại"),
    FEEDBACK("FEEDBACK", "Phản hồi"),
    SHARE("SHARE", "Chia sẻ"),
    REVOKE("REVOKE", "Thu hồi chia sẻ");

    String key;
    String name;
}
