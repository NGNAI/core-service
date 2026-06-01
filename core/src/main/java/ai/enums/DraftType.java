package ai.enums;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)    
public enum DraftType {
    EMAIL("email", "Email", "Bản nháp email chuyên nghiệp và trực tiếp."),
    REPORT("report", "Báo cáo", "Bản nháp báo cáo chi tiết, mang tính phân tích."),
    PROPOSAL("proposal", "Đề xuất", "Bản nháp đề xuất dự án hoặc ý tưởng."),
    PLAN("plan", "Kế hoạch", "Bản nháp kế hoạch hành động chi tiết."),
    ARTICLE("article", "Bài viết", "Bản nháp bài viết blog hoặc bài báo."),
    SOCIAL_POST("social_post", "Bài đăng mạng xã hội", "Bản nháp nội dung cho mạng xã hội."),
    SCRIPT("script", "Kịch bản", "Bản nháp kịch bản video hoặc thuyết trình."),
    MEETING_NOTE("meeting_note", "Ghi chú cuộc họp", "Bản nháp ghi chú cuộc họp."),
    OTHER("other", "Khác", "Các loại bản nháp khác.");

    String value;
    String caption;
    String description;

    DraftType(String value, String caption, String description) {
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
                .anyMatch(type -> type.getValue().equalsIgnoreCase(value));
    }
}
