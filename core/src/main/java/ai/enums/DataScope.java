package ai.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DataScope {
    PERSONAL("Cho cá nhân"), // Cho cá nhân
    LOCAL("Cho tổ chức"), // Cho tổ chức
    GLOBAL("Cho toàn hệ thống"); // Cho toàn hệ thống

    private final String key;
    private final String value;

    DataScope(String value) {
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
