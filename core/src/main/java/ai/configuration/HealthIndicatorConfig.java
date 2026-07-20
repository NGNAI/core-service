package ai.configuration;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import ai.AppProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Cấu hình các custom HealthIndicator cho external dependencies:
 * <ul>
 *   <li>{@code minio} — kiểm tra kết nối MinIO</li>
 *   <li>{@code ragApi} — kiểm tra RagAPI endpoint /health</li>
 *   <li>{@code ingestionApi} — kiểm tra IngestionAPI endpoint /health</li>
 * </ul>
 * Các indicator này xuất hiện trong Spring Boot Actuator {@code /actuator/health}
 * và được Spring Boot Admin UI hiển thị chi tiết.
 */
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Configuration
public class HealthIndicatorConfig {

    AppProperties appProperties;

    /**
     * Health indicator cho MinIO — kiểm tra kết nối bằng BucketExistsArgs.
     * Trạng thái UP khi kết nối thành công, DOWN khi có lỗi.
     */
    @Bean
    public HealthIndicator minioHealthIndicator() {
        return () -> {
            AppProperties.Minio minio = appProperties.getMinio();
            if (minio == null || minio.getEndpoint() == null || minio.getEndpoint().isBlank()) {
                return Health.down().withDetail("error", "MinIO endpoint chưa cấu hình").build();
            }
            try {
                MinioClient client = MinioClient.builder()
                        .endpoint(minio.getEndpoint())
                        .credentials(minio.getAccessKey(), minio.getSecretKey())
                        .build();
                boolean connected = client.bucketExists(BucketExistsArgs.builder().bucket("health-check").build());
                // bucketExists trả về true nếu bucket tồn tại, false nếu không — cả 2 đều chứng tỏ kết nối OK
                Health.Builder builder = Health.up();
                builder.withDetail("endpoint", minio.getEndpoint());
                builder.withDetail("bucketExists", connected);
                return builder.build();
            } catch (Exception e) {
                log.warn("MinIO health check thất bại: {}", e.getMessage());
                return Health.down(e)
                        .withDetail("endpoint", minio.getEndpoint())
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * Health indicator cho RagAPI — gọi GET /health.
     * RagAPI response: {@code {"status":"ok"}}
     * Trạng thái UP khi response chứa status = "ok", DOWN khi lỗi hoặc response không hợp lệ.
     */
    @Bean
    public HealthIndicator ragApiHealthIndicator(WebClient ragWebClient) {
        return () -> {
            AppProperties.Rag rag = appProperties.getRag();
            if (rag == null || rag.getUrl() == null || rag.getUrl().isBlank()) {
                return Health.down().withDetail("error", "RagAPI URL chưa cấu hình").build();
            }
            try {
                Map<String, Object> response = ragWebClient.get()
                        .uri("/health")
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block(Duration.ofSeconds(10));
                boolean ok = response != null && "ok".equalsIgnoreCase(String.valueOf(response.get("status")));
                Health.Builder builder = ok ? Health.up() : Health.down();
                builder.withDetail("url", rag.getUrl() + "/health");
                if (response != null) {
                    builder.withDetail("response", response);
                }
                return builder.build();
            } catch (Exception e) {
                log.warn("RagAPI health check thất bại: {}", e.getMessage());
                return Health.down(e)
                        .withDetail("url", rag.getUrl() + "/health")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * Health indicator cho IngestionAPI — gọi GET /health.
     * IngestionAPI response: {@code {"status":"ok","version":"4.0.0","role":"ingestion"}}
     * Trạng thái UP khi response chứa status = "ok", DOWN khi lỗi hoặc response không hợp lệ.
     */
    @Bean
    public HealthIndicator ingestionApiHealthIndicator(RestClient ingestionRestClient) {
        return () -> {
            AppProperties.Ingestion ingestion = appProperties.getIngestion();
            if (ingestion == null || ingestion.getUrl() == null || ingestion.getUrl().isBlank()) {
                return Health.down().withDetail("error", "IngestionAPI URL chưa cấu hình").build();
            }
            try {
                Map<String, Object> response = ingestionRestClient.get()
                        .uri("/health")
                        .retrieve()
                        .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
                boolean ok = response != null && "ok".equalsIgnoreCase(String.valueOf(response.get("status")));
                Health.Builder builder = ok ? Health.up() : Health.down();
                builder.withDetail("url", ingestion.getUrl() + "/health");
                if (response != null) {
                    builder.withDetail("response", response);
                }
                return builder.build();
            } catch (Exception e) {
                log.warn("IngestionAPI health check thất bại: {}", e.getMessage());
                return Health.down(e)
                        .withDetail("url", ingestion.getUrl() + "/health")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
}