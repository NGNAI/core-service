package ai.service;

import ai.AppProperties;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class MinioService {
    AppProperties appProperties;

    // Tải file lên Minio và trả về object path để lưu vào database, object path này sẽ được sử dụng để truy xuất file sau này
    public String upload(MultipartFile file, String unit, String username) {
        try {
            AppProperties.Minio minio = appProperties.getMinio();
            MinioClient client = MinioClient.builder()
                    .endpoint(minio.getEndpoint())
                    .credentials(minio.getAccessKey(), minio.getSecretKey())
                    .build();

            String bucket = minio.getBucket();
            ensureBucket(client, bucket);

            String objectPath = buildObjectPath(unit, username, file.getOriginalFilename());
            try (InputStream inputStream = file.getInputStream()) {
                client.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectPath)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            return objectPath;
        } catch (Exception exception) {
            throw new AppException(ApiResponseStatus.MEDIA_UPLOAD_FAILED);
        }
    }

    // Tải file từ Minio theo object path và trả về dữ liệu dưới dạng byte array cùng với content type
    public MinioObjectData download(String objectPath) {
        try {
            AppProperties.Minio minio = appProperties.getMinio();
            MinioClient client = MinioClient.builder()
                    .endpoint(minio.getEndpoint())
                    .credentials(minio.getAccessKey(), minio.getSecretKey())
                    .build();

            String contentType = client.statObject(
                StatObjectArgs.builder()
                    .bucket(minio.getBucket())
                    .object(objectPath)
                    .build()
            ).contentType();

            try (InputStream inputStream = client.getObject(
                    GetObjectArgs.builder()
                            .bucket(minio.getBucket())
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
            throw new AppException(ApiResponseStatus.MEDIA_DOWNLOAD_FAILED);
        }
    }

    // Tao URL có chữ ký để tải file trực tiếp từ Minio, URL này sẽ hết hạn sau expiresInSeconds giây
    public String generatePresignedDownloadUrl(String objectPath, int expiresInSeconds) {
        try {
            AppProperties.Minio minio = appProperties.getMinio();
            MinioClient client = MinioClient.builder()
                    .endpoint(minio.getEndpoint())
                    .credentials(minio.getAccessKey(), minio.getSecretKey())
                    .build();

            int effectiveExpiry = expiresInSeconds > 0 ? expiresInSeconds : 900;

            return client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minio.getBucket())
                            .object(objectPath)
                            .expiry(effectiveExpiry, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception exception) {
            throw new AppException(ApiResponseStatus.MEDIA_DOWNLOAD_FAILED);
        }
    }

    // Kiểm tra nếu bucket không tồn tại thì tạo mới
    private void ensureBucket(MinioClient client, String bucket) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    // Xây dựng đường dẫn đối tượng theo định dạng: unit/username/yyyy/MM/dd/safeFileName
    private String buildObjectPath(String unit, String username, String originalFilename) {
        LocalDate now = LocalDate.now();
        String safeFileName = buildSafeFileName(originalFilename);
        return unit + "/" + username + "/" + now.getYear() + "/" + now.getMonthValue() + "/"  + now.getDayOfMonth() + "/" + safeFileName;
    }

    // Tạo tên file an toàn bằng cách loại bỏ các ký tự không hợp lệ và thêm UUID để tránh trùng lặp
    private String buildSafeFileName(String originalFilename) {
        String source = (originalFilename == null || originalFilename.isBlank()) ? "file.bin" : originalFilename;
        String normalized = source.replace("\\", "_").replace("/", "_").replace(" ", "_");
        return UUID.randomUUID() + "-" + normalized;
    }

    // Chuẩn hóa content type, nếu null hoặc rỗng thì mặc định là application/octet-stream
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
