package ai.service;

import ai.dto.outer.ingestion.response.IngestionStatusResponseDto;
import ai.dto.outer.ingestion.response.IngestionUploadResponseDto;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class IngestionService {
    private static final String INGESTION_UPLOAD_PATH = "/upload";
    private static final String INGESTION_STATUS_PATH = "/job";

    RestClient ingestionRestClient;

    public IngestionService(@Qualifier("ingestionRestClient") RestClient ingestionRestClient) {
        this.ingestionRestClient = ingestionRestClient;
    }

    public IngestionUploadResponseDto pushToVector(MultipartFile file, String username, String unit, String visibility) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        body.add("username", username);
        body.add("unit", unit);
        body.add("visibility", visibility);

        try {
            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientException exception) {
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    public IngestionUploadResponseDto pushToVector(byte[] fileBytes, String fileName, String username, String unit, String visibility) {
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

        try {
            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientException exception) {
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    public IngestionStatusResponseDto getJobStatus(UUID jobId) {
        try {
            return ingestionRestClient.get()
                    .uri(INGESTION_STATUS_PATH + "/{jobId}", jobId)
                    .retrieve()
                    .body(IngestionStatusResponseDto.class);
        } catch (RestClientException exception) {
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }
}
