package ai.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DataSource {
    SYSTEM("Của hệ thống"), 
    DOCUMENT("Cho tài liệu"), 
    TOPIC("Cho chủ đề"),
    NOTEBOOK("Cho sổ tay");

    private final String key;
    private final String value;

    DataSource(String value) {
        this.key = name();
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
