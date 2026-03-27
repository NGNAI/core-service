package ai.service;

import ai.dto.outer.ingestion.response.IngestionStatusResponseDto;
import ai.dto.outer.ingestion.response.IngestionUploadResponseDto;
import ai.enums.ApiResponseStatus;
import ai.enums.DataScope;
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
    private static final String INGESTION_DELETE_PATH = "/file";

    RestClient ingestionRestClient;

    public IngestionService(@Qualifier("ingestionRestClient") RestClient ingestionRestClient) {
        this.ingestionRestClient = ingestionRestClient;
    }

    /**
     * Đẩy file cần ingest lên service ingestion để xử lý chuyển đổi thành vector. Service ingestion sẽ trả về jobId để có thể poll trạng thái xử lý sau này
     * @param file
     * @param fileId
     * @param username
     * @param uniId
     * @param unitName
     * @param visibility
     * @return
     */
    public IngestionUploadResponseDto pushToVector(MultipartFile file, String fileId, String username, String uniId, String unitName, DataScope visibility) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        body.add("file_id", fileId);
        body.add("user_name", username);
        body.add("unit_id", uniId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());

        try {
            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Overload method của pushToVector để hỗ trợ trường hợp file đã được đọc thành byte array trong bộ nhớ, tránh phải đọc lại file từ disk khi đã có sẵn byte array (ví dụ trường hợp file đã được upload lên MinIO và đọc về dưới dạng byte array để đẩy tiếp lên ingestion service)
     * @param fileBytes
     * @param fileName
     * @param fileId
     * @param username
     * @param unitId
     * @param unitName
     * @param visibility
     * @return
     */
    public IngestionUploadResponseDto pushToVector(byte[] fileBytes, String fileName, String fileId, String username, String unitId, String unitName, DataScope visibility) {
        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("file_id", fileId);
        body.add("user_name", username);
        body.add("unit_id", unitId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());

        try {
            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Poll trạng thái xử lý ingestion job bằng jobId trả về từ phương thức pushToVector. Thông thường sẽ cần gọi phương thức này nhiều lần sau khi gọi pushToVector để theo dõi tiến độ xử lý của ingestion job, cho đến khi trạng thái trả về là success hoặc failed thì thôi
     * @param jobId
     * @return
     */
    public IngestionStatusResponseDto getJobStatus(UUID jobId) {
        try {
            return ingestionRestClient.get()
                    .uri(INGESTION_STATUS_PATH + "/{jobId}", jobId)
                    .retrieve()
                    .body(IngestionStatusResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Xóa một file đã được gửi lên ingestion service bằng fileId. Thông thường sẽ cần gọi phương thức này khi muốn hủy một job đang chờ xử lý hoặc đang xử lý trên ingestion service, hoặc muốn xóa một file đã được xử lý xong trên ingestion service nhưng không muốn giữ lại kết quả vector của file đó nữa
     * @param fileId
     * @return
     */
    public IngestionStatusResponseDto deleteFile(String fileId) {
        try {
            return ingestionRestClient.delete()
                    .uri(INGESTION_DELETE_PATH + "/{fileId}", fileId)
                    .retrieve()
                    .body(IngestionStatusResponseDto.class);
        } catch (RestClientException exception) {
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }
}
