package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Phạm vi của prompt template.
 * <ul>
 *   <li>{@code SYSTEM} — do admin tạo, dùng chung cho tất cả org (global), không gắn với user/org cụ thể.</li>
 *   <li>{@code USER} — do người dùng tự tạo, gắn với user (owner) và org của user.</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum PromptScope {
    SYSTEM("SYSTEM", "Hệ thống"),
    USER("USER", "Người dùng");

    String key;
    String name;
}
