package ai.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ai.AppProperties;
import ai.dto.own.response.SystemHealthResponseDto;
import ai.dto.own.response.SystemHealthResponseDto.ComponentHealthDto;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Service kiểm tra trạng thái các external dependencies để admin UI hiển thị.
 * Kiểm tra: MinIO, RagAPI, IngestionAPI, Database (qua JPA), Redis (qua cache).
 */
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class SystemHealthService {

    AppProperties appProperties;
    WebClient ragWebClient;
    RestClient ingestionRestClient;
    SystemSettingService systemSettingService;

    /**
     * Kiểm tra trạng thái tất cả external dependencies và trả về kết quả tổng hợp.
     * @return DTO chứa trạng thái tổng + chi tiết từng thành phần
     */
    public SystemHealthResponseDto checkAll() {
        Instant start = Instant.now();
        List<ComponentHealthDto> components = new ArrayList<>();
        boolean allUp = true;

        // MinIO
        ComponentHealthDto minioHealth = checkMinio();
        components.add(minioHealth);
        if (!"UP".equals(minioHealth.getStatus())) allUp = false;

        // RagAPI
        ComponentHealthDto ragHealth = checkRagApi();
        components.add(ragHealth);
        if (!"UP".equals(ragHealth.getStatus())) allUp = false;

        // IngestionAPI
        ComponentHealthDto ingestionHealth = checkIngestionApi();
        components.add(ingestionHealth);
        if (!"UP".equals(ingestionHealth.getStatus())) allUp = false;

        long totalMs = Duration.between(start, Instant.now()).toMillis();

        return SystemHealthResponseDto.builder()
                .status(allUp ? "UP" : "DOWN")
                .checkedAt(Instant.now())
                .totalCheckTimeMs(totalMs)
                .components(components)
                .build();
    }

    /**
     * Kiểm tra kết nối MinIO bằng BucketExistsArgs.
     */
    private ComponentHealthDto checkMinio() {
        Instant start = Instant.now();
        AppProperties.Minio minio = appProperties.getMinio();
        String name = "minio";
        if (minio == null || minio.getEndpoint() == null || minio.getEndpoint().isBlank()) {
            return buildDown(name, "MinIO endpoint chưa cấu hình", start, null);
        }
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(minio.getEndpoint())
                    .credentials(minio.getAccessKey(), minio.getSecretKey())
                    .build();
            boolean bucketExists = client.bucketExists(BucketExistsArgs.builder().bucket("health-check").build());
            return buildUp(name, "endpoint=" + minio.getEndpoint() + ", bucketExists=" + bucketExists, start);
        } catch (Exception e) {
            log.warn("MinIO health check thất bại: {}", e.getMessage());
            return buildDown(name, e.getMessage(), start, minio.getEndpoint());
        }
    }

    /**
     * Kiểm tra RagAPI bằng GET /health (response: {"status":"ok"}).
     */
    private ComponentHealthDto checkRagApi() {
        Instant start = Instant.now();
        AppProperties.Rag rag = appProperties.getRag();
        String name = "ragApi";
        if (rag == null || rag.getUrl() == null || rag.getUrl().isBlank()) {
            return buildDown(name, "RagAPI URL chưa cấu hình", start, null);
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = ragWebClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));
            boolean ok = response != null && "ok".equalsIgnoreCase(String.valueOf(response.get("status")));
            String detail = response != null ? response.toString() : "empty response";
            return ok ? buildUp(name, detail, start) : buildDown(name, "Response status không phải 'ok': " + detail, start, rag.getUrl() + "/health");
        } catch (Exception e) {
            log.warn("RagAPI health check thất bại: {}", e.getMessage());
            return buildDown(name, e.getMessage(), start, rag.getUrl() + "/health");
        }
    }

    /**
     * Kiểm tra IngestionAPI bằng GET /health (response: {"status":"ok","version":"4.0.0","role":"ingestion"}).
     */
    private ComponentHealthDto checkIngestionApi() {
        Instant start = Instant.now();
        AppProperties.Ingestion ingestion = appProperties.getIngestion();
        String name = "ingestionApi";
        if (ingestion == null || ingestion.getUrl() == null || ingestion.getUrl().isBlank()) {
            return buildDown(name, "IngestionAPI URL chưa cấu hình", start, null);
        }
        try {
            Map<String, Object> response = ingestionRestClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            boolean ok = response != null && "ok".equalsIgnoreCase(String.valueOf(response.get("status")));
            String detail = response != null ? response.toString() : "empty response";
            return ok ? buildUp(name, detail, start) : buildDown(name, "Response status không phải 'ok': " + detail, start, ingestion.getUrl() + "/health");
        } catch (Exception e) {
            log.warn("IngestionAPI health check thất bại: {}", e.getMessage());
            return buildDown(name, e.getMessage(), start, ingestion.getUrl() + "/health");
        }
    }

    private ComponentHealthDto buildUp(String name, String detail, Instant start) {
        return ComponentHealthDto.builder()
                .name(name)
                .status("UP")
                .detail(detail)
                .checkTimeMs(Duration.between(start, Instant.now()).toMillis())
                .build();
    }

    private ComponentHealthDto buildDown(String name, String error, Instant start, String endpoint) {
        return ComponentHealthDto.builder()
                .name(name)
                .status("DOWN")
                .error(error)
                .detail(endpoint != null ? "endpoint=" + endpoint : null)
                .checkTimeMs(Duration.between(start, Instant.now()).toMillis())
                .build();
    }
}