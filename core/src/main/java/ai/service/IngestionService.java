package ai.service;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.annotation.Audited;
import ai.dto.outer.ingestion.response.IngestionDeleteResponseDto;
import ai.dto.outer.ingestion.response.IngestionStatusResponseDto;
import ai.dto.outer.ingestion.response.IngestionSummaryResponseDto;
import ai.dto.outer.ingestion.response.IngestionUploadResponseDto;
import ai.enums.ApiResponseStatus;
import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.enums.DataScope;
import ai.exception.AppException;
import ai.exception.IngestionServiceException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
@Slf4j
public class IngestionService {
    private static final String INGESTION_UPLOAD_RAG_PATH = "/upload_rag";
    private static final String INGESTION_UPLOAD_CHAT_PATH = "/upload_chat";
    private static final String INGESTION_UPLOAD_NOTEBOOK_PATH = "/upload_notebook";
    private static final String INGESTION_STATUS_PATH = "/job";
    private static final String INGESTION_DELETE_FILE_RAG_PATH = "/file_rag";
    private static final String INGESTION_DELETE_FILE_CHAT_PATH = "/file_chat";
    private static final String INGESTION_DELETE_FILE_NOTEBOOK_PATH = "/file_notebook";
    private static final String INGESTION_SUMMARY_FILE_NOTEBOOK_PATH = "/summarize";

    RestClient ingestionRestClient;
    ObjectMapper objectMapper;

    public IngestionService(@Qualifier("ingestionRestClient") RestClient ingestionRestClient, ObjectMapper objectMapper) {
        this.ingestionRestClient = ingestionRestClient;
        this.objectMapper = objectMapper;
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
    @Audited(action = AuditAction.UPLOAD, resource = AuditResource.DATA_INGESTION, description = "Upload RAG file: {0}")
    public IngestionUploadResponseDto uploadRag(MultipartFile file, String fileId, String userId, String username, String uniId, String unitName, DataScope visibility) {
        return uploadRag(file, fileId, userId, username, uniId, unitName, visibility, null);
    }

    @Audited(action = AuditAction.UPLOAD, resource = AuditResource.DATA_INGESTION, description = "Upload RAG file: {0}")
    public IngestionUploadResponseDto uploadRag(MultipartFile file, String fileId, String userId, String username, String uniId, String unitName, DataScope visibility, String callbackUrl) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        body.add("file_id", fileId);
        body.add("user_id", userId);
        body.add("user_name", username);
        body.add("unit_id", uniId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(bodyWithoutFile(body));
            log.info("INGESTION POST {} request body:\n{}", INGESTION_UPLOAD_RAG_PATH, prettyPrint(jsonBody));

            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_RAG_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientResponseException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getResponseBodyAsString());
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage());
        } catch (JsonProcessingException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }

    /**
     * Overload method của pushToVector để hỗ trợ trường hợp file đã được đọc thành byte array trong bộ nhớ, tránh phải đọc lại file từ disk khi đã có sẵn byte array (ví dụ trường hợp file đã được upload lên MinIO và đọc về dưới dạng byte array để đẩy tiếp lên ingestion service)
     * @param fileBytes
     * @param fileName
     * @param fileId
     * @param userId
     * @param username
     * @param unitId
     * @param unitName
     * @param visibility
     * @return
     */
    @Audited(action = AuditAction.UPLOAD, resource = AuditResource.DATA_INGESTION, description = "Upload RAG file: {0}")
    public IngestionUploadResponseDto uploadRag(byte[] fileBytes, String fileName, String fileId, String userId, String username, String unitId, String unitName, DataScope visibility) {
        return uploadRag(fileBytes, fileName, fileId, userId, username, unitId, unitName, visibility, null);
    }

    @Audited(action = AuditAction.UPLOAD, resource = AuditResource.DATA_INGESTION, description = "Upload RAG file: {0}")
    public IngestionUploadResponseDto uploadRag(byte[] fileBytes, String fileName, String fileId, String userId, String username, String unitId, String unitName, DataScope visibility, String callbackUrl) {
        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("file_id", fileId);
        body.add("user_id", userId);
        body.add("user_name", username);
        body.add("unit_id", unitId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(bodyWithoutFile(body));
            log.info("INGESTION POST {} request body:\n{}", INGESTION_UPLOAD_RAG_PATH, prettyPrint(jsonBody));

            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_RAG_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientResponseException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getResponseBodyAsString());
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage());
        } catch (JsonProcessingException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage()); 
        }
    }

    /**
     * Overload method để đẩy file từ InputStream (streaming) lên ingestion service, tránh load toàn bộ file vào RAM với file lớn.
     * @param inputStream
     * @param size
     * @param fileName
     * @param fileId
     * @param userId
     * @param username
     * @param unitId
     * @param unitName
     * @param visibility
     * @param callbackUrl
     * @return
     */
    @Audited(action = AuditAction.UPLOAD, resource = AuditResource.DATA_INGESTION, description = "Upload RAG file: {0}")
    public IngestionUploadResponseDto uploadRag(InputStream inputStream, long size, String fileName, String fileId, String userId, String username, String unitId, String unitName, DataScope visibility, String callbackUrl) {
        InputStreamResource fileResource = newInputStreamResource(inputStream, size, fileName);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("file_id", fileId);
        body.add("user_id", userId);
        body.add("user_name", username);
        body.add("unit_id", unitId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(bodyWithoutFile(body));
            log.info("INGESTION POST {} request body:\n{}", INGESTION_UPLOAD_RAG_PATH, prettyPrint(jsonBody));

            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_RAG_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientResponseException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getResponseBodyAsString());
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage());
        } catch (JsonProcessingException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }

    /**
     * Tạo InputStreamResource có hỗ trợ contentLength để Spring multipart stream file mà không cần buffer toàn bộ vào RAM.
     */
    private InputStreamResource newInputStreamResource(InputStream inputStream, long size, String fileName) {
        return new InputStreamResource(inputStream) {
            @Override
            public long contentLength() {
                return size;
            }

            @Override
            public String getFilename() {
                return fileName;
            }
        };
    }

    /**
     * Overload method để đẩy file trực tiếp từ disk (FileSystemResource) lên ingestion service,
     * tránh load toàn bộ file vào RAM với file lớn.
     * @param file
     * @param fileName
     * @param fileId
     * @param userId
     * @param username
     * @param unitId
     * @param unitName
     * @param visibility
     * @param callbackUrl
     * @return
     */
    @Audited(action = AuditAction.UPLOAD, resource = AuditResource.DATA_INGESTION, description = "Upload RAG file: {0}")
    public IngestionUploadResponseDto uploadRag(File file, String fileName, String fileId, String userId, String username, String unitId, String unitName, DataScope visibility, String callbackUrl) {
        FileSystemResource fileResource = new FileSystemResource(file) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("file_id", fileId);
        body.add("user_id", userId);
        body.add("user_name", username);
        body.add("unit_id", unitId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(bodyWithoutFile(body));
            log.info("INGESTION POST {} request body:\n{}", INGESTION_UPLOAD_RAG_PATH, prettyPrint(jsonBody));

            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_RAG_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientResponseException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getResponseBodyAsString());
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage());
        } catch (JsonProcessingException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }


    @Audited(action = AuditAction.UPLOAD, resource = AuditResource.DATA_INGESTION, description = "Upload chat file: {0}")
    public IngestionUploadResponseDto uploadChat(MultipartFile file, String fileId, String userId, String username, String uniId, String unitName, DataScope visibility, String topicId) {
        return uploadChat(file, fileId, userId, username, uniId, unitName, visibility, topicId, null);
    }

    @Audited(action = AuditAction.UPLOAD, resource = AuditResource.DATA_INGESTION, description = "Upload chat file: {0}")
    public IngestionUploadResponseDto uploadChat(MultipartFile file, String fileId, String userId, String username, String uniId, String unitName, DataScope visibility, String topicId, String callbackUrl) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        body.add("file_id", fileId);
        body.add("user_id", userId);
        body.add("user_name", username);
        body.add("unit_id", uniId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());
        body.add("topic_id", topicId);
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(bodyWithoutFile(body));
            log.info("INGESTION POST {} request body:\n{}", INGESTION_UPLOAD_CHAT_PATH, prettyPrint(jsonBody));

            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_CHAT_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        } catch (JsonProcessingException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }

    @Audited(action = AuditAction.UPLOAD, resource = AuditResource.DATA_INGESTION, description = "Upload chat file: {0}")
    public IngestionUploadResponseDto uploadChat(InputStream inputStream, long size, String fileName, String fileId, String userId, String username, String unitId, String unitName, DataScope visibility, String topicId, String callbackUrl) {
        InputStreamResource fileResource = newInputStreamResource(inputStream, size, fileName);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("file_id", fileId);
        body.add("user_id", userId);
        body.add("user_name", username);
        body.add("unit_id", unitId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());
        body.add("topic_id", topicId);
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(bodyWithoutFile(body));
            log.info("INGESTION POST {} request body:\n{}", INGESTION_UPLOAD_CHAT_PATH, prettyPrint(jsonBody));

            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_CHAT_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        } catch (JsonProcessingException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }

    public IngestionUploadResponseDto uploadChat(byte[] fileBytes, String fileName, String fileId, String userId, String username, String unitId, String unitName, DataScope visibility, String topicId) {
        return uploadChat(fileBytes, fileName, fileId, userId, username, unitId, unitName, visibility, topicId, null);
    }

    @Audited(action = AuditAction.UPLOAD, resource = AuditResource.DATA_INGESTION, description = "Upload chat file: {0}")
    public IngestionUploadResponseDto uploadChat(byte[] fileBytes, String fileName, String fileId, String userId, String username, String unitId, String unitName, DataScope visibility, String topicId, String callbackUrl) {
        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("file_id", fileId);
        body.add("user_id", userId);
        body.add("user_name", username);
        body.add("unit_id", unitId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());
        body.add("topic_id", topicId);
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(bodyWithoutFile(body));
            log.info("INGESTION POST {} request body:\n{}", INGESTION_UPLOAD_CHAT_PATH, prettyPrint(jsonBody));

            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_CHAT_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        } catch (JsonProcessingException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }  

    @Audited(action = AuditAction.UPLOAD, resource = AuditResource.DATA_INGESTION, description = "Upload notebook file: {0}")
    public IngestionUploadResponseDto uploadNoteBook(MultipartFile file, String fileId, String userId, String username, String uniId, String unitName, DataScope visibility, String notebookId) {
        return uploadNoteBook(file, fileId, userId, username, uniId, unitName, visibility, notebookId, null);
    }

    @Audited(action = AuditAction.UPLOAD, resource = AuditResource.DATA_INGESTION, description = "Upload notebook file: {0}")
    public IngestionUploadResponseDto uploadNoteBook(MultipartFile file, String fileId, String userId, String username, String uniId, String unitName, DataScope visibility, String notebookId, String callbackUrl) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        body.add("file_id", fileId);
        body.add("user_id", userId);
        body.add("user_name", username);
        body.add("unit_id", uniId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());
        body.add("notebook_id", notebookId);
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(bodyWithoutFile(body));
            log.info("INGESTION POST {} request body:\n{}", INGESTION_UPLOAD_NOTEBOOK_PATH, prettyPrint(jsonBody));

            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_NOTEBOOK_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        } catch (JsonProcessingException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }

    @Audited(action = AuditAction.UPLOAD, resource = AuditResource.DATA_INGESTION, description = "Upload notebook file: {0}")
    public IngestionUploadResponseDto uploadNoteBook(InputStream inputStream, long size, String fileName, String fileId, String userId, String username, String unitId, String unitName, DataScope visibility, String notebookId, String callbackUrl) {
        InputStreamResource fileResource = newInputStreamResource(inputStream, size, fileName);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("file_id", fileId);
        body.add("user_id", userId);
        body.add("user_name", username);
        body.add("unit_id", unitId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());
        body.add("notebook_id", notebookId);
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(bodyWithoutFile(body));
            log.info("INGESTION POST {} request body:\n{}", INGESTION_UPLOAD_NOTEBOOK_PATH, prettyPrint(jsonBody));

            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_NOTEBOOK_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        } catch (JsonProcessingException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }

    public IngestionUploadResponseDto uploadNoteBook(byte[] fileBytes, String fileName, String fileId, String userId, String username, String unitId, String unitName, DataScope visibility, String notebookId) {
        return uploadNoteBook(fileBytes, fileName, fileId, userId, username, unitId, unitName, visibility, notebookId, null);
    }

    @Audited(action = AuditAction.UPLOAD, resource = AuditResource.DATA_INGESTION, description = "Upload notebook file: {0}")
    public IngestionUploadResponseDto uploadNoteBook(byte[] fileBytes, String fileName, String fileId, String userId, String username, String unitId, String unitName, DataScope visibility, String notebookId, String callbackUrl) {
        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("file_id", fileId);
        body.add("user_id", userId);
        body.add("user_name", username);
        body.add("unit_id", unitId);
        body.add("unit_name", unitName);
        body.add("visibility", visibility.name());
        body.add("notebook_id", notebookId);
        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            body.add("callback_url", callbackUrl.trim());
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(bodyWithoutFile(body));
            log.info("INGESTION POST {} request body:\n{}", INGESTION_UPLOAD_NOTEBOOK_PATH, prettyPrint(jsonBody));

            return ingestionRestClient.post()
                    .uri(INGESTION_UPLOAD_NOTEBOOK_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(IngestionUploadResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        } catch (JsonProcessingException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }






    /**
     * Poll trạng thái xử lý ingestion job bằng jobId trả về từ phương thức pushToVector. Thông thường sẽ cần gọi phương thức này nhiều lần sau khi gọi pushToVector để theo dõi tiến độ xử lý của ingestion job, cho đến khi trạng thái trả về là success hoặc failed thì thôi
     * @param jobId
     * @return
     */
    @Audited(action = AuditAction.READ, resource = AuditResource.DATA_INGESTION, description = "Get ingestion job status: {0}")
    public IngestionStatusResponseDto getJobStatus(UUID jobId) {
        try {
            log.info("INGESTION GET {} request for jobId: {}", INGESTION_STATUS_PATH, jobId);

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
    @Audited(action = AuditAction.DELETE, resource = AuditResource.DATA_INGESTION, description = "Delete RAG file: {0}")
    public IngestionDeleteResponseDto deleteFileRag(String fileId) {
        try {
            log.info("INGESTION DELETE {} request for fileId: {}", INGESTION_DELETE_FILE_RAG_PATH, fileId);

            return ingestionRestClient.delete()
                    .uri(INGESTION_DELETE_FILE_RAG_PATH + "/{fileId}", fileId)
                    .retrieve()
                    .body(IngestionDeleteResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Xóa một file chat đã được gửi lên ingestion service bằng fileId. Thông thường sẽ cần gọi phương thức này khi muốn hủy một job đang chờ xử lý hoặc đang xử lý trên ingestion service, hoặc muốn xóa một file đã được xử lý xong trên ingestion service nhưng không muốn giữ lại kết quả vector của file đó nữa
     * @param fileId
     * @return
     */
    @Audited(action = AuditAction.DELETE, resource = AuditResource.DATA_INGESTION, description = "Delete chat file: {0}")
    public IngestionDeleteResponseDto deleteFileChat(String fileId) {
        try {
            log.info("INGESTION DELETE {} request for fileId: {}", INGESTION_DELETE_FILE_CHAT_PATH, fileId);

            return ingestionRestClient.delete()
                    .uri(INGESTION_DELETE_FILE_CHAT_PATH + "/{fileId}", fileId)
                    .retrieve()
                    .body(IngestionDeleteResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Xóa một file notebook đã được gửi lên ingestion service bằng fileId. Thông thường sẽ cần gọi phương thức này khi muốn hủy một job đang chờ xử lý hoặc đang xử lý trên ingestion service, hoặc muốn xóa một file đã được xử lý xong trên ingestion service nhưng không muốn giữ lại kết quả vector của file đó nữa
     * @param fileId
     * @return
     */
    @Audited(action = AuditAction.DELETE, resource = AuditResource.DATA_INGESTION, description = "Delete notebook file: {0}")
    public IngestionDeleteResponseDto deleteFileNotebook(String fileId) {
        try {
            log.info("INGESTION DELETE {} request for fileId: {}", INGESTION_DELETE_FILE_NOTEBOOK_PATH, fileId);
            
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
    @Audited(action = AuditAction.READ, resource = AuditResource.DATA_INGESTION, description = "Get ingestion summary: {0}")
    public IngestionSummaryResponseDto getIngestionSummary(String fileId) {
        try {
            Map<String, Object> body = Map.of(
                "file_id", fileId,
                "collection_type", "notebook"
            );

            String bodyJson = objectMapper.writeValueAsString(body);
            log.info("INGESTION POST {} request body:\n{}", INGESTION_SUMMARY_FILE_NOTEBOOK_PATH, prettyPrint(bodyJson));

            return ingestionRestClient.post()
                    .uri(INGESTION_SUMMARY_FILE_NOTEBOOK_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(IngestionSummaryResponseDto.class);
        } catch (RestClientException exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
        } catch (JsonProcessingException exception) {
            exception.printStackTrace();
            throw new IngestionServiceException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }

    /**
     * Tạo bản sao chỉ chứa metadata (loại trừ phần binary "file") của multipart body
     * để có thể JSON-serialize an toàn cho mục đích logging, tránh lỗi
     * InvalidDefinitionException khi Jackson cố serialize InputStream/Resource.
     */
    private Map<String, Object> bodyWithoutFile(MultiValueMap<String, Object> body) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        body.forEach((key, values) -> {
            if (!"file".equals(key)) {
                metadata.put(key, values != null && values.size() == 1 ? values.get(0) : values);
            }
        });
        return metadata;
    }

    private String prettyPrint(String json) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(json));
        } catch (JsonProcessingException e) {
            return json;
        }
    }
}
