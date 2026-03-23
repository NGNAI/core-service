package ai.enums;

public enum MediaUploadTarget {
    INGESTION, // Dùng cho việc upload media để ingest vào hệ thống, không liên quan đến bất kỳ entity nào
    TOPIC, // Dùng cho việc upload media liên quan đến Topic, có thể là ảnh đại diện, tài liệu đính kèm, v.v. Topic sẽ có một trường mediaId để liên kết với MediaEntity
}
