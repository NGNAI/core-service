package ai.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ai.enums.ApiResponseStatus;
import ai.exception.AppException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
@Slf4j
public class MinioService {
    MinioClient minioClient;

    /**
     * Tải file MultipartFile lên Minio.
     * Đường dẫn: unit/username/yyyy/MM/dd/safeFileName
     */
    public String upload(MultipartFile file, String username, String unit, String bucketName) {
        try {
            ensureBucket(bucketName);

            String objectPath = buildObjectPath(username, unit, file.getOriginalFilename());
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                               .bucket(bucketName)
                                .object(objectPath)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            return objectPath;
        } catch (Exception exception) {
            log.error("Failed to upload file to Minio: {}/{}", bucketName, username, exception);
            throw new AppException(ApiResponseStatus.DATA_INGESTION_UPLOAD_FAILED);
        }
    }

    /**
     * Tải byte array lên Minio.
     */
    public String upload(byte[] bytes, String fileName, String contentType, String username, String unit, String bucketName) {
        try {
            ensureBucket(bucketName);

            String objectPath = buildObjectPath(username, unit, fileName);
            try (InputStream inputStream = new java.io.ByteArrayInputStream(bytes)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectPath)
                                .stream(inputStream, bytes.length, -1)
                                .contentType(normalizeContentType(contentType))
                                .build()
                );
            }

            return objectPath;
        } catch (Exception exception) {
            log.error("Failed to upload bytes to Minio: {}/{}", bucketName, username, exception);
            throw new AppException(ApiResponseStatus.DATA_INGESTION_UPLOAD_FAILED);
        }
    }

    /**
     * Tải file từ Minio về dưới dạng byte array.
     */
    public MinioObjectData download(String objectPath, String bucketName) {
        try {
            String contentType = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectPath)
                    .build()
            ).contentType();

            try (InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectPath)
                        .build())) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                return new MinioObjectData(
                        outputStream.toByteArray(),
                        normalizeContentType(contentType)
                );
            }
        } catch (Exception exception) {
            log.error("Failed to download object from Minio: {}/{}", bucketName, objectPath, exception);
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DOWNLOAD_FAILED);
        }
    }

    /**
     * Xóa file khỏi Minio.
     */
    public void delete(String objectPath, String bucketName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectPath)
                        .build()
            );
        } catch (Exception exception) {
            log.error("Failed to delete object from Minio: {}/{}", bucketName, objectPath, exception);
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DELETE_FAILED);
        }
    }

    /**
     * Tạo presigned URL để download file.
     */
    public String generatePresignedDownloadUrl(String objectPath, int expiresInSeconds, String bucketName) {
        try {
            int effectiveExpiry = expiresInSeconds > 0 ? expiresInSeconds : 900;

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                        .bucket(bucketName)
                            .object(objectPath)
                            .expiry(effectiveExpiry, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception exception) {
            log.error("Failed to generate presigned URL for Minio object: {}/{}", bucketName, objectPath, exception);
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DOWNLOAD_FAILED);
        }
    }

    /**
     * Đảm bảo bucket tồn tại.
     */
    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    /**
     * Xây dựng đường dẫn đối tượng theo định dạng: unit/username/yyyy/MM/dd/safeFileName
     * @param username
     * @param unit
     * @param originalFilename
     * @return
     */
    private String buildObjectPath(String username, String unit, String originalFilename) {
        LocalDate now = LocalDate.now();
        String safeFileName = buildSafeFileName(originalFilename);
        return unit + "/" + username + "/" + now.getYear() + "/" + now.getMonthValue() + "/"  + now.getDayOfMonth() + "/" + safeFileName;
    }

    /**
     * Hàm này sẽ nhận tên file gốc và trả về một tên file đã được chuẩn hóa và an toàn để lưu trữ trong Minio. Hàm sẽ loại bỏ các ký tự không hợp lệ như dấu gạch chéo, dấu cách, v.v. và thay thế chúng bằng dấu gạch dưới. Ngoài ra, hàm cũng sẽ thêm một UUID vào đầu tên file để đảm bảo tính duy nhất và tránh trùng lặp khi nhiều file có cùng tên gốc được tải lên.
     * @param originalFilename
     * @return
     */
    private String buildSafeFileName(String originalFilename) {
        String source = (originalFilename == null || originalFilename.isBlank()) ? "file.bin" : originalFilename;
        String normalized = source.replace("\\", "_").replace("/", "_").replace(" ", "_");
        return UUID.randomUUID() + "-" + normalized;
    }

    /**
     * Chuẩn hóa content type, nếu null hoặc rỗng thì mặc định là application/octet-stream
     * @param contentType
     * @return
     */
    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    @Data
    @AllArgsConstructor
    public static class MinioObjectData {
        byte[] bytes;
        String contentType;
    }
}
