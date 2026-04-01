package ai;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
    AutoIngestion autoIngestion;

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
    }
    
    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Ingestion {
        String url;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Minio {
        String endpoint;
        String accessKey;
        String secretKey;
        String bucket;
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
        boolean deleteLocalAfterSuccess;
    }
}