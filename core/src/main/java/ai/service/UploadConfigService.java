package ai.service;

import java.util.Arrays;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ai.dto.own.response.UploadConfigResponseDto;
import ai.enums.ApiResponseStatus;
import ai.enums.UploadType;
import ai.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Dịch vụ đọc cấu hình giới hạn upload và validate file theo từng loại
 * (Topic/Notebook/Draft/Data-Ingestion).
 *
 * <p>Cấu hình được lưu trong System Settings (DB động, admin chỉnh qua UI),
 * đọc qua {@link SystemSettingService} với default fallback hằng số.
 * Quy ước: giá trị = 0 nghĩa là không giới hạn (unlimited).</p>
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class UploadConfigService {

    // ========================================================================
    // Default fallback (dùng khi setting chưa được seed hoặc không active)
    // ========================================================================
    static final int DEFAULT_TOPIC_MAX_FILE_COUNT = 3;
    static final int DEFAULT_TOPIC_MAX_FILE_SIZE_MB = 100;
    static final int DEFAULT_NOTEBOOK_MAX_FILE_COUNT = 10;
    static final int DEFAULT_NOTEBOOK_MAX_FILE_SIZE_MB = 100;
    static final int DEFAULT_NOTEBOOK_MAX_TOTAL_SOURCES = 100;
    static final int DEFAULT_DRAFT_MAX_FILE_COUNT = 3;
    static final int DEFAULT_DRAFT_MAX_FILE_SIZE_MB = 100;
    static final int DEFAULT_DATA_INGESTION_MAX_FILE_COUNT = 10;
    static final int DEFAULT_DATA_INGESTION_MAX_FILE_SIZE_MB = 100;

    // ========================================================================
    // Setting keys
    // ========================================================================
    static final String KEY_TOPIC_MAX_FILE_COUNT = "upload.topic.maxFileCount";
    static final String KEY_TOPIC_MAX_FILE_SIZE_MB = "upload.topic.maxFileSizeMb";
    static final String KEY_TOPIC_ALLOWED_FILE_TYPES = "upload.topic.allowedFileTypes";
    static final String KEY_NOTEBOOK_MAX_FILE_COUNT = "upload.notebook.maxFileCount";
    static final String KEY_NOTEBOOK_MAX_FILE_SIZE_MB = "upload.notebook.maxFileSizeMb";
    static final String KEY_NOTEBOOK_MAX_TOTAL_SOURCES = "upload.notebook.maxTotalSources";
    static final String KEY_NOTEBOOK_ALLOWED_FILE_TYPES = "upload.notebook.allowedFileTypes";
    static final String KEY_DRAFT_MAX_FILE_COUNT = "upload.draft.maxFileCount";
    static final String KEY_DRAFT_MAX_FILE_SIZE_MB = "upload.draft.maxFileSizeMb";
    static final String KEY_DRAFT_ALLOWED_FILE_TYPES = "upload.draft.allowedFileTypes";
    static final String KEY_DATA_INGESTION_MAX_FILE_COUNT = "upload.dataIngestion.maxFileCount";
    static final String KEY_DATA_INGESTION_MAX_FILE_SIZE_MB = "upload.dataIngestion.maxFileSizeMb";
    static final String KEY_DATA_INGESTION_ALLOWED_FILE_TYPES = "upload.dataIngestion.allowedFileTypes";

    SystemSettingService systemSettingService;

    /**
     * Lấy cấu hình giới hạn upload cho một loại.
     * @param type loại upload
     * @return cấu hình (giá trị 0 = unlimited)
     */
    public UploadConfigResponseDto getConfig(UploadType type) {
        if (type == null) {
            throw new AppException(ApiResponseStatus.INVALID_UPLOAD_TYPE);
        }
        switch (type) {
            case TOPIC:
                return UploadConfigResponseDto.builder()
                        .type(type.name())
                        .maxFileCount(getInt(KEY_TOPIC_MAX_FILE_COUNT, DEFAULT_TOPIC_MAX_FILE_COUNT))
                        .maxFileSizeMb(getInt(KEY_TOPIC_MAX_FILE_SIZE_MB, DEFAULT_TOPIC_MAX_FILE_SIZE_MB))
                        .allowedFileTypes(getString(KEY_TOPIC_ALLOWED_FILE_TYPES, ""))
                        .build();
            case NOTEBOOK:
                return UploadConfigResponseDto.builder()
                        .type(type.name())
                        .maxFileCount(getInt(KEY_NOTEBOOK_MAX_FILE_COUNT, DEFAULT_NOTEBOOK_MAX_FILE_COUNT))
                        .maxFileSizeMb(getInt(KEY_NOTEBOOK_MAX_FILE_SIZE_MB, DEFAULT_NOTEBOOK_MAX_FILE_SIZE_MB))
                        .maxTotalSources(getInt(KEY_NOTEBOOK_MAX_TOTAL_SOURCES, DEFAULT_NOTEBOOK_MAX_TOTAL_SOURCES))
                        .allowedFileTypes(getString(KEY_NOTEBOOK_ALLOWED_FILE_TYPES, ""))
                        .build();
            case DRAFT:
                return UploadConfigResponseDto.builder()
                        .type(type.name())
                        .maxFileCount(getInt(KEY_DRAFT_MAX_FILE_COUNT, DEFAULT_DRAFT_MAX_FILE_COUNT))
                        .maxFileSizeMb(getInt(KEY_DRAFT_MAX_FILE_SIZE_MB, DEFAULT_DRAFT_MAX_FILE_SIZE_MB))
                        .allowedFileTypes(getString(KEY_DRAFT_ALLOWED_FILE_TYPES, ""))
                        .build();
            case DATA_INGESTION:
                return UploadConfigResponseDto.builder()
                        .type(type.name())
                        .maxFileCount(getInt(KEY_DATA_INGESTION_MAX_FILE_COUNT, DEFAULT_DATA_INGESTION_MAX_FILE_COUNT))
                        .maxFileSizeMb(getInt(KEY_DATA_INGESTION_MAX_FILE_SIZE_MB, DEFAULT_DATA_INGESTION_MAX_FILE_SIZE_MB))
                        .allowedFileTypes(getString(KEY_DATA_INGESTION_ALLOWED_FILE_TYPES, ""))
                        .build();
            default:
                throw new AppException(ApiResponseStatus.INVALID_UPLOAD_TYPE);
        }
    }

    /**
     * Validate số lượng file trong một lần upload. Giá trị max = 0 nghĩa là unlimited.
     * @param type loại upload
     * @param fileCount số lượng file hợp lệ (đã lọc null/empty)
     * @throws AppException nếu vượt giới hạn
     */
    public void validateFileCount(UploadType type, int fileCount) {
        int max = getMaxFileCount(type);
        if (max > 0 && fileCount > max) {
            throw new AppException(ApiResponseStatus.FILE_COUNT_EXCEEDED);
        }
    }

    /**
     * Validate dung lượng và loại file. Giá trị max = 0 nghĩa là unlimited.
     * @param type loại upload
     * @param file file cần kiểm tra
     * @throws AppException nếu vượt giới hạn dung lượng hoặc loại file không được phép
     */
    public void validateFile(UploadType type, MultipartFile file) {
        if (file == null || file.isEmpty()) return;

        int maxSizeMb = getMaxFileSizeMb(type);
        if (maxSizeMb > 0) {
            long maxSizeBytes = (long) maxSizeMb * 1024 * 1024;
            if (file.getSize() > maxSizeBytes) {
                throw new AppException(ApiResponseStatus.FILE_SIZE_EXCEEDED);
            }
        }

        String allowedTypes = getAllowedFileTypes(type);
        if (allowedTypes != null && !allowedTypes.isBlank()) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                throw new AppException(ApiResponseStatus.FILE_TYPE_NOT_ALLOWED);
            }
            String extension = extractExtension(originalFilename);
            if (extension == null || extension.isBlank()) {
                throw new AppException(ApiResponseStatus.FILE_TYPE_NOT_ALLOWED);
            }
            String[] allowedExtensions = allowedTypes.toLowerCase(Locale.ROOT).split("\\s*,\\s*");
            boolean allowed = false;
            for (String ext : allowedExtensions) {
                if (ext.equals(extension.toLowerCase(Locale.ROOT))) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                throw new AppException(ApiResponseStatus.FILE_TYPE_NOT_ALLOWED);
            }
        }
    }

    /**
     * Validate tổng số source (chỉ áp dụng cho Notebook). Giá trị max = 0 nghĩa là unlimited.
     * @param type loại upload
     * @param currentCount số source hiện có
     * @param newCount số source mới thêm trong lần này
     * @throws AppException nếu tổng vượt giới hạn
     */
    public void validateTotalSources(UploadType type, long currentCount, int newCount) {
        if (type != UploadType.NOTEBOOK) return;
        int max = getInt(KEY_NOTEBOOK_MAX_TOTAL_SOURCES, DEFAULT_NOTEBOOK_MAX_TOTAL_SOURCES);
        if (max > 0 && (currentCount + newCount) > max) {
            throw new AppException(ApiResponseStatus.TOTAL_SOURCES_EXCEEDED);
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private int getMaxFileCount(UploadType type) {
        switch (type) {
            case TOPIC: return getInt(KEY_TOPIC_MAX_FILE_COUNT, DEFAULT_TOPIC_MAX_FILE_COUNT);
            case NOTEBOOK: return getInt(KEY_NOTEBOOK_MAX_FILE_COUNT, DEFAULT_NOTEBOOK_MAX_FILE_COUNT);
            case DRAFT: return getInt(KEY_DRAFT_MAX_FILE_COUNT, DEFAULT_DRAFT_MAX_FILE_COUNT);
            case DATA_INGESTION: return getInt(KEY_DATA_INGESTION_MAX_FILE_COUNT, DEFAULT_DATA_INGESTION_MAX_FILE_COUNT);
            default: throw new AppException(ApiResponseStatus.INVALID_UPLOAD_TYPE);
        }
    }

    private int getMaxFileSizeMb(UploadType type) {
        switch (type) {
            case TOPIC: return getInt(KEY_TOPIC_MAX_FILE_SIZE_MB, DEFAULT_TOPIC_MAX_FILE_SIZE_MB);
            case NOTEBOOK: return getInt(KEY_NOTEBOOK_MAX_FILE_SIZE_MB, DEFAULT_NOTEBOOK_MAX_FILE_SIZE_MB);
            case DRAFT: return getInt(KEY_DRAFT_MAX_FILE_SIZE_MB, DEFAULT_DRAFT_MAX_FILE_SIZE_MB);
            case DATA_INGESTION: return getInt(KEY_DATA_INGESTION_MAX_FILE_SIZE_MB, DEFAULT_DATA_INGESTION_MAX_FILE_SIZE_MB);
            default: throw new AppException(ApiResponseStatus.INVALID_UPLOAD_TYPE);
        }
    }

    private String getAllowedFileTypes(UploadType type) {
        switch (type) {
            case TOPIC: return getString(KEY_TOPIC_ALLOWED_FILE_TYPES, "");
            case NOTEBOOK: return getString(KEY_NOTEBOOK_ALLOWED_FILE_TYPES, "");
            case DRAFT: return getString(KEY_DRAFT_ALLOWED_FILE_TYPES, "");
            case DATA_INGESTION: return getString(KEY_DATA_INGESTION_ALLOWED_FILE_TYPES, "");
            default: throw new AppException(ApiResponseStatus.INVALID_UPLOAD_TYPE);
        }
    }

    private int getInt(String key, int defaultValue) {
        return systemSettingService.getInt(key, defaultValue);
    }

    private String getString(String key, String defaultValue) {
        return systemSettingService.getString(key, defaultValue);
    }

    /**
     * Lấy extension (phần mở rộng) từ tên file, không bao gồm dấu chấm.
     * @param filename tên file gốc
     * @return extension hoặc null nếu không có
     */
    private String extractExtension(String filename) {
        if (filename == null) return null;
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) return null;
        return filename.substring(dotIndex + 1);
    }
}
