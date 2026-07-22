package ai;

import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import ai.enums.DataSource;
import ai.enums.DataScope;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Component
@ConfigurationProperties
public class AppProperties {
    Jwt jwt;
    Otp otp;
    Rag rag;
    Ingestion ingestion;
    Minio minio;
    Integration integration;
    AutoIngestion autoIngestion;
    Ldap ldap;
    Security security;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Jwt {
        String secretKey;
        long expiryDuration;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Otp {
        String url;
        String xApiKey;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Rag {
        String url;
        Memory memory;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Memory {
        Integer topicRecentMessageWindow;
        Integer noteBookRecentMessageWindow;
        Integer minMessagesToCompress;
    }
    
    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Ingestion {
        String url;
        Long readTimeoutMs;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Minio {
        String endpoint;
        String accessKey;
        String secretKey;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Integration {
        AttachmentApi attachmentApi;
        DataIngestionApi dataIngestionApi;
        DataIngestionCallback dataIngestionCallback;
        NotebookSourceCallback notebookSourceCallback;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AttachmentApi {
        String headerName;
        String key;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DataIngestionApi {
        String headerName;
        String key;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DataIngestionCallback {
        String url;
        String signature;
        String signatureParamName;
        String signatureHeaderName;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class NotebookSourceCallback {
        String url;
        String signature;
        String signatureParamName;
        String signatureHeaderName;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AutoIngestion {
        boolean enabled;
        String inputDir;
        String processingDir;
        String failedDir;
        Long pollerDelayMs;
        Long fileStableSeconds;
        DataScope accessLevel;
        DataSource fromSource;
        boolean deleteLocalAfterSuccess;
    }

    /**
     * Cấu hình tích hợp LDAP (qua OTP Service trung gian).
     */
    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Ldap {
        /**
         * UUID của organization mặc định để gán user LDAP mới (khi login lần đầu hoặc auto-sync).
         * Để trống nếu không muốn auto-assign.
         */
        String defaultOrgId;

        /**
         * UUID của role mặc định khi gán user LDAP vào organization.
         * Để trống → fallback về {@code RoleRepository.findByDefaultAssign()}.
         */
        String defaultRoleId;

        /**
         * Có cập nhật thông tin user (email, fullName, phoneNumber) từ OTP Service khi login lại không.
         * Mặc định false để tránh ghi đè thông tin admin đã chỉnh trong hệ thống.
         */
        boolean updateOnLogin;

        /**
         * Cấu hình auto-sync user LDAP từ OTP Service về DB local.
         */
        Sync sync;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Sync {
        /**
         * Bật/tắt scheduler đồng bộ user LDAP định kỳ.
         */
        boolean enabled;

        /**
         * Cron expression cho scheduler đồng bộ.
         * Mặc định: 2h sáng mỗi ngày.
         */
        String cron;
    }

    /**
     * Cấu hình bảo mật bổ sung (không thuộc hệ thống RBAC).
     */
    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Security {
        /**
         * Danh sách username được phép truy cập các resource admin nhạy cảm
         * (SYSTEM_SETTING, SYSTEM_HEALTH) mà không phụ thuộc Role/Permission.
         * Để trống/null → fallback về ["root"].
         */
        List<String> adminAllowedUsernames;
    }
}