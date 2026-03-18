package ai.service;

import ai.AppProperties;
import ai.dto.outer.ingestion.response.IngestionStatusResponseDto;
import ai.dto.outer.ingestion.response.IngestionUploadResponseDto;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class IngestionService {
    private static final String INGESTION_UPLOAD_PATH = "/upload";
    private static final String INGESTION_STATUS_PATH = "/job";

    RestTemplate restTemplate;
    AppProperties appProperties;

    public IngestionUploadResponseDto pushToVector(MultipartFile file, String username, String unit, String visibility) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        body.add("username", username);
        body.add("unit", unit);
        body.add("visibility", visibility);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            return restTemplate.postForObject(
                ingestionBaseUrl() + INGESTION_UPLOAD_PATH,
                    requestEntity,
                    IngestionUploadResponseDto.class
            );
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    public IngestionUploadResponseDto pushToVector(byte[] fileBytes, String fileName, String username, String unit, String visibility) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("username", username);
        body.add("unit", unit);
        body.add("visibility", visibility);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            return restTemplate.postForObject(
                ingestionBaseUrl() + INGESTION_UPLOAD_PATH,
                    requestEntity,
                    IngestionUploadResponseDto.class
            );
        } catch (RestClientException exception) {
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    public IngestionStatusResponseDto getJobStatus(UUID jobId) {
        try {
            String url = ingestionBaseUrl() + INGESTION_STATUS_PATH + "/" + jobId;
            return restTemplate.getForObject(url, IngestionStatusResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    private String ingestionBaseUrl() {
        String url = appProperties.getIngestion().getUrl();
        if (url == null || url.isBlank()) {
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
