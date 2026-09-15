package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Loại đối tượng chatbot mà prompt template được dùng cho.
 * <p>
 * Mỗi prompt chỉ thuộc <b>đúng một</b> loại. Prompt dùng chung cho nhiều chatbot thì tạo
 * nhiều record (mỗi loại một record) — không dùng giá trị gộp kiểu "BOTH".
 * Cách này giữ enum mở rộng được: thêm loại chatbot mới chỉ cần thêm 1 hằng số,
 * không phải sửa lại ý nghĩa các giá trị cũ.
 * <ul>
 *   <li>{@code TOPIC} — dùng cho chat với Topic.</li>
 *   <li>{@code NOTEBOOK} — dùng cho chat với NotebookLM.</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum PromptType {
    TOPIC("TOPIC", "Chủ đề"),
    NOTEBOOK("NOTEBOOK", "Sổ tay");

    String key;
    String name;
}
