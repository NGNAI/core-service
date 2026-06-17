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
    REPORT("report", "Báo cáo", "Bản nháp báo cáo chi tiết, mang tính phân tích."),
    PROPOSAL("proposal", "Đề xuất", "Bản nháp đề xuất dự án hoặc ý tưởng."),
    ANNOUNCEMENT("announcement", "Thông báo", "Bản nháp thông báo chính thức hoặc nội bộ."),
    MEMO("memo", "Công văn/Bản ghi nhớ", "Bản nháp công văn hoặc bản ghi nhớ nội bộ."),
    CONTRACT("contract", "Hợp đồng", "Bản nháp hợp đồng pháp lý hoặc thỏa thuận."),
    SUMMARY("summary", "Tóm tắt", "Bản nháp tóm tắt tài liệu dài hoặc cuộc họp."),
    PLAN("plan", "Kế hoạch", "Bản nháp kế hoạch hành động chi tiết."),
    LETTER("letter", "Thư", "Bản nháp thư cá nhân hoặc công việc."),
    GENERAL("general", "Tổng quát", "Bản nháp văn bản tổng quát không thuộc loại cụ thể.");

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
