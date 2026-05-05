package ai.service;

import ai.dto.outer.ingestion.response.IngestionDeleteResponseDto;
import ai.dto.outer.ingestion.response.IngestionStatusResponseDto;
import ai.dto.outer.ingestion.response.IngestionSummaryResponseDto;
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
    private static final String INGESTION_UPLOAD_RAG_PATH = "/upload_rag";
    private static final String INGESTION_UPLOAD_CHAT_PATH = "/upload_chat";
    private static final String INGESTION_UPLOAD_NOTEBOOK_PATH = "/upload_notebook";
    private static final String INGESTION_STATUS_PATH = "/job";
    private static final String INGESTION_DELETE_FILE_RAG_PATH = "/file_rag";
    private static final String INGESTION_DELETE_FILE_CHAT_PATH = "/file_chat";
    private static final String INGESTION_DELETE_FILE_NOTEBOOK_PATH = "/file_notebook";
    private static final String INGESTION_SUMMARY_FILE_NOTEBOOK_PATH = "/file_notebook/summary";

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
    public IngestionUploadResponseDto uploadRag(MultipartFile file, String fileId, String username, String uniId, String unitName, DataScope visibility) {
        return uploadRag(file, fileId, username, uniId, unitName, visibility, null);
    }

    public IngestionUploadResponseDto uploadRag(MultipartFile file, String fileId, String username, String uniId, String unitName, DataScope visibility, String callbackUrl) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        body.add("file_id", fileId);
        body.add("user_name", username);
        body.add("unit_id", uniId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_RAG_PATH)
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
    public IngestionUploadResponseDto uploadRag(byte[] fileBytes, String fileName, String fileId, String username, String unitId, String unitName, DataScope visibility) {
        return uploadRag(fileBytes, fileName, fileId, username, unitId, unitName, visibility, null);
    }

    public IngestionUploadResponseDto uploadRag(byte[] fileBytes, String fileName, String fileId, String username, String unitId, String unitName, DataScope visibility, String callbackUrl) {
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
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_RAG_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }


    public IngestionUploadResponseDto uploadChat(MultipartFile file, String fileId, String username, String uniId, String unitName, DataScope visibility, String topicId) {
        return uploadChat(file, fileId, username, uniId, unitName, visibility, topicId, null);
    }

    public IngestionUploadResponseDto uploadChat(MultipartFile file, String fileId, String username, String uniId, String unitName, DataScope visibility, String topicId, String callbackUrl) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        body.add("file_id", fileId);
        body.add("user_name", username);
        body.add("unit_id", uniId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());
        body.add("topic_id", topicId);
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_CHAT_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    public IngestionUploadResponseDto uploadChat(byte[] fileBytes, String fileName, String fileId, String username, String unitId, String unitName, DataScope visibility, String topicId) {
        return uploadChat(fileBytes, fileName, fileId, username, unitId, unitName, visibility, topicId, null);
    }

    public IngestionUploadResponseDto uploadChat(byte[] fileBytes, String fileName, String fileId, String username, String unitId, String unitName, DataScope visibility, String topicId, String callbackUrl) {
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
        body.add("topic_id", topicId);
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_CHAT_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }  

    public IngestionUploadResponseDto uploadNoteBook(MultipartFile file, String fileId, String username, String uniId, String unitName, DataScope visibility, String notebookId) {
        return uploadNoteBook(file, fileId, username, uniId, unitName, visibility, notebookId, null);
    }

    public IngestionUploadResponseDto uploadNoteBook(MultipartFile file, String fileId, String username, String uniId, String unitName, DataScope visibility, String notebookId, String callbackUrl) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        body.add("file_id", fileId);
        body.add("user_name", username);
        body.add("unit_id", uniId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());
        body.add("notebook_id", notebookId);
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_NOTEBOOK_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    public IngestionUploadResponseDto uploadNoteBook(byte[] fileBytes, String fileName, String fileId, String username, String unitId, String unitName, DataScope visibility, String notebookId) {
        return uploadNoteBook(fileBytes, fileName, fileId, username, unitId, unitName, visibility, notebookId, null);
    }

    public IngestionUploadResponseDto uploadNoteBook(byte[] fileBytes, String fileName, String fileId, String username, String unitId, String unitName, DataScope visibility, String notebookId, String callbackUrl) {
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
        body.add("notebook_id", notebookId);
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_NOTEBOOK_PATH)
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
    public IngestionDeleteResponseDto deleteFileRag(String fileId) {
        try {
            return ingestionRestClient.delete()
                    .uri(INGESTION_DELETE_FILE_RAG_PATH + "/{fileId}", fileId)
                    .retrieve()
                    .body(IngestionDeleteResponseDto.class);
        } catch (RestClientException exception) {
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Xóa một file chat đã được gửi lên ingestion service bằng fileId. Thông thường sẽ cần gọi phương thức này khi muốn hủy một job đang chờ xử lý hoặc đang xử lý trên ingestion service, hoặc muốn xóa một file đã được xử lý xong trên ingestion service nhưng không muốn giữ lại kết quả vector của file đó nữa
     * @param fileId
     * @return
     */
    public IngestionDeleteResponseDto deleteFileChat(String fileId) {
        try {
            return ingestionRestClient.delete()
                    .uri(INGESTION_DELETE_FILE_CHAT_PATH + "/{fileId}", fileId)
                    .retrieve()
                    .body(IngestionDeleteResponseDto.class);
        } catch (RestClientException exception) {
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Xóa một file notebook đã được gửi lên ingestion service bằng fileId. Thông thường sẽ cần gọi phương thức này khi muốn hủy một job đang chờ xử lý hoặc đang xử lý trên ingestion service, hoặc muốn xóa một file đã được xử lý xong trên ingestion service nhưng không muốn giữ lại kết quả vector của file đó nữa
     * @param fileId
     * @return
     */
    public IngestionDeleteResponseDto deleteFileNotebook(String fileId) {
        try {
            return ingestionRestClient.delete()
                    .uri(INGESTION_DELETE_FILE_NOTEBOOK_PATH + "/{fileId}", fileId)
                    .retrieve()
                    .body(IngestionDeleteResponseDto.class);
        } catch (RestClientException exception) {
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Lấy thông tin tóm tắt (summary) của một file notebook đã được gửi lên ingestion service bằng fileId. Thông thường sẽ cần gọi phương thức này sau khi đã nhận được jobId từ phương thức uploadNoteBook và poll trạng thái xử lý của job đó bằng phương thức getJobStatus, khi thấy trạng thái xử lý đã hoàn thành thành công (success) thì sẽ gọi phương thức này để lấy thông tin tóm tắt của file notebook đó, thông tin tóm tắt này có thể bao gồm các trường như số lượng chunk đã được chia nhỏ từ file gốc, số lượng vector đã được tạo ra, v.v. Tóm tắt này sẽ giúp người dùng có cái nhìn tổng quan về kết quả xử lý của file notebook trên ingestion service mà không cần phải truy cập trực tiếp vào database vector store để kiểm tra
     * @param fileId
     * @return
     */
    public IngestionSummaryResponseDto getIngestionSummary(String fileId) {
        try {
            return ingestionRestClient.get()
                    .uri(INGESTION_SUMMARY_FILE_NOTEBOOK_PATH + "/{fileId}", fileId)
                    .retrieve()
                    .body(IngestionSummaryResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }
}
