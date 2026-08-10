package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Loại đối tượng chatbot mà prompt template được dùng cho.
 * <ul>
 *   <li>{@code TOPIC} — chỉ dùng cho chat với Topic.</li>
 *   <li>{@code NOTEBOOK} — chỉ dùng cho chat với NotebookLM.</li>
 *   <li>{@code BOTH} — dùng được cho cả Topic lẫn NotebookLM.</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum PromptType {
    TOPIC("TOPIC", "Chủ đề"),
    NOTEBOOK("NOTEBOOK", "Sổ tay"),
    BOTH("BOTH", "Cả hai");

    String key;
    String name;
}
