package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum AuditResource {
    USER("USER", "Người dùng"),
    ORG("ORG", "Tổ chức"),
    ROLE("ROLE", "Vai trò"),
    PERMISSION("PERMISSION", "Quyền"),
    ORG_USER_ROLE("ORG_USER_ROLE", "Phân quyền người dùng trong tổ chức"),
    DATA_INGESTION("DATA_INGESTION", "Dữ liệu"),
    NOTE("NOTE", "Ghi chú"),
    NOTEBOOK("NOTEBOOK", "Sổ tay"),
    NOTEBOOK_SOURCE("NOTEBOOK_SOURCE", "Nguồn sổ tay"),
    TOPIC("TOPIC", "Chủ đề"),
    TOPIC_SOURCE("TOPIC_SOURCE", "Nguồn chủ đề"),
    DRAFT("DRAFT", "Bản nháp"),
    DRAFT_VERSION("DRAFT_VERSION", "Phiên bản bản nháp"),
    MESSAGE("MESSAGE", "Tin nhắn"),
    CATEGORY("CATEGORY", "Danh mục"),
    ATTACHMENT("ATTACHMENT", "Tệp đính kèm"),
    AUTH("AUTH", "Xác thực"),
    SYSTEM_SETTING("SYSTEM_SETTING", "Cấu hình hệ thống"),
    SYSTEM_HEALTH("SYSTEM_HEALTH", "Trạng thái hệ thống"),
    SHARE_LINK("SHARE_LINK", "Link chia sẻ");

    String key;
    String name;
}
