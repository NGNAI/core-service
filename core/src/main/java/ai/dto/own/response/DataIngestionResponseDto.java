package ai.dto.own.response;

import java.util.UUID;

import ai.enums.DataScope;
import ai.enums.DataSource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataIngestionResponseDto extends AuditResponseDto {
    UUID id;
    String name;
    boolean folder;
    String contentType;
    Long fileSize;
    String minioPath;
    UUID parentId;
    UUID ownerId;
    UUID orgId;
    DataScope accessLevel;
    DataSource fromSource;
    UUID jobId;
    String ingestionStatus;
    String deleteStatus;
    // Số lần retry thất bại liên tiếp khi đẩy sang ingestion service (RAG), dùng để giới hạn retry trong auto-import
    Integer retryCount;
    // Thông báo lỗi từ ingestion service (RAG) khi lần ingest gần nhất thất bại,
    // lưu nguyên body response dạng string để tiện tra cứu nguyên nhân khi debug
    String ingestionError;
}
