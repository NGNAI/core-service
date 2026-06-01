package ai.enums;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DraftPresentationStyle {
    FORMAL("formal", "Trang trọng", "Phong cách chuyên nghiệp, phù hợp với văn bản công việc chính thức."),
    FRIENDLY("friendly", "Thân mật", "Phong cách gần gũi, phù hợp với bạn bè hoặc đồng nghiệp thân thiết."),
    PERSUASIVE("persuasive", "Thuyết phục", "Phong cách nhằm mục đích thuyết phục người đọc hành động."),
    CONCISE("concise", "Ngắn gọn", "Phong cách cô đọng, đi thẳng vào vấn đề."),
    STORYTELLING("storytelling", "Kể chuyện", "Phong cách kể chuyện, tạo sự cuốn hút và cảm xúc."),
    TECHNICAL("technical", "Kỹ thuật", "Phong cách chuyên sâu, sử dụng thuật ngữ chuyên ngành."),
    BULLET_POINT("bullet_point", "Gạch đầu dòng", "Phong cách liệt kê bằng các gạch đầu dòng, dễ đọc."),
    EXECUTIVE_SUMMARY("executive_summary", "Tóm tắt điều hành", "Tóm tắt các điểm chính cho cấp quản lý."),
    OTHER("other", "Khác", "Các kiểu trình bày khác.");

    String value;
    String caption;
    String description;

    DraftPresentationStyle(String value, String caption, String description) {
        this.value = value;
        this.caption = caption;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getCaption() {
        return caption;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isSupportedValue(String value) {
        return Arrays.stream(values())
                .anyMatch(style -> style.getValue().equalsIgnoreCase(value));
    }
}
