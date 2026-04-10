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
import io.minio.RemoveObjectArgs;
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

    /**
     * Hàm này sẽ nhận một file MultipartFile cùng với thông tin về username, unit và bucketName để tải file đó lên Minio. Hàm sẽ xây dựng đường dẫn đối tượng trong Minio theo định dạng: unit/username/yyyy/MM/dd/safeFileName, trong đó safeFileName là tên file đã được chuẩn hóa và thêm UUID để đảm bảo tính duy nhất. Sau đó, hàm sẽ sử dụng MinioClient để kiểm tra sự tồn tại của bucket và tạo bucket nếu cần thiết, rồi tải file lên Minio. Nếu quá trình tải file thành công, hàm sẽ trả về đường dẫn đối tượng trong Minio. Nếu có lỗi xảy ra trong quá trình tải file, hàm sẽ ném ra AppException với mã lỗi DATA_INGESTION_UPLOAD_FAILED.
     * @param file
     * @param username
     * @param unit
     * @param bucketName
     * @return
     */
    public String upload(MultipartFile file, String username, String unit, String bucketName) {
        try {
            AppProperties.Minio minio = appProperties.getMinio();
            MinioClient client = MinioClient.builder()
                    .endpoint(minio.getEndpoint())
                    .credentials(minio.getAccessKey(), minio.getSecretKey())
                    .build();

            String bucket = bucketName;
            ensureBucket(client, bucket);

            String objectPath = buildObjectPath(username, unit, file.getOriginalFilename());
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
            throw new AppException(ApiResponseStatus.DATA_INGESTION_UPLOAD_FAILED);
        }
    }

    /**
     * Hàm này sẽ nhận đường dẫn đối tượng trong Minio và bucketName, sau đó tải file đó về từ Minio và trả về dữ liệu file dưới dạng byte array cùng với content type của file. Hàm sẽ sử dụng MinioClient để gọi API tải file về từ Minio. Nếu có lỗi xảy ra trong quá trình tải file, hàm sẽ ném ra AppException với mã lỗi DATA_INGESTION_DOWNLOAD_FAILED.
     * @param bytes
     * @param fileName
     * @param contentType
     * @param username
     * @param unit
     * @param bucketName
     * @return
     */
    public String upload(byte[] bytes, String fileName, String contentType, String username, String unit, String bucketName) {
        try {
            AppProperties.Minio minio = appProperties.getMinio();
            MinioClient client = MinioClient.builder()
                    .endpoint(minio.getEndpoint())
                    .credentials(minio.getAccessKey(), minio.getSecretKey())
                    .build();

            String bucket = bucketName;
            ensureBucket(client, bucket);

            String objectPath = buildObjectPath(username, unit, fileName);
            try (InputStream inputStream = new java.io.ByteArrayInputStream(bytes)) {
                client.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectPath)
                                .stream(inputStream, bytes.length, -1)
                                .contentType(normalizeContentType(contentType))
                                .build()
                );
            }

            return objectPath;
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.DATA_INGESTION_UPLOAD_FAILED);
        }
    }

    /**
     * Hàm này sẽ nhận đường dẫn đối tượng trong Minio và bucketName, sau đó tải file đó về từ Minio và trả về dữ liệu file dưới dạng byte array cùng với content type của file. Hàm sẽ sử dụng MinioClient để gọi API tải file về từ Minio. Nếu có lỗi xảy ra trong quá trình tải file, hàm sẽ ném ra AppException với mã lỗi DATA_INGESTION_DOWNLOAD_FAILED.
     * @param objectPath
     * @param bucketName
     * @return
     */
    public MinioObjectData download(String objectPath, String bucketName) {
        try {
            AppProperties.Minio minio = appProperties.getMinio();
            MinioClient client = MinioClient.builder()
                    .endpoint(minio.getEndpoint())
                    .credentials(minio.getAccessKey(), minio.getSecretKey())
                    .build();

            String bucket = bucketName;

            String contentType = client.statObject(
                StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectPath)
                    .build()
            ).contentType();

            try (InputStream inputStream = client.getObject(
                    GetObjectArgs.builder()
                        .bucket(bucket)
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
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DOWNLOAD_FAILED);
        }
    }

    /**
     * Hàm này sẽ nhận đường dẫn đối tượng trong Minio và bucketName, sau đó xóa file đó khỏi Minio. Hàm sẽ sử dụng MinioClient để gọi API xóa file khỏi Minio. Nếu có lỗi xảy ra trong quá trình xóa file, hàm sẽ ném ra AppException với mã lỗi DATA_INGESTION_DELETE_FAILED.
     * @param objectPath
     * @param bucketName
     */
    public void delete(String objectPath, String bucketName) {
        try {
            AppProperties.Minio minio = appProperties.getMinio();
            MinioClient client = MinioClient.builder()
                    .endpoint(minio.getEndpoint())
                    .credentials(minio.getAccessKey(), minio.getSecretKey())
                    .build();

            String bucket = bucketName;

            client.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(bucket)
                            .object(objectPath)
                            .build()
            );
        } catch (Exception exception) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DELETE_FAILED);
        }
    }

    /**
     * Hàm này sẽ nhận object path của file trong Minio và thời gian hết hạn tính bằng giây, sau đó tạo và trả về URL có chữ ký (presigned URL) để tải file đó về. URL này sẽ có hiệu lực trong khoảng thời gian được chỉ định bởi expiresInSeconds. Hàm này sẽ sử dụng MinioClient để gọi API tạo presigned URL của Minio. Nếu có lỗi trong quá trình tạo presigned URL, hàm sẽ ném ra AppException với mã lỗi DATA_INGESTION_DOWNLOAD_FAILED.
     * @param objectPath
     * @param expiresInSeconds
     * @return
     */
    public String generatePresignedDownloadUrl(String objectPath, int expiresInSeconds, String bucketName) {
        try {
            AppProperties.Minio minio = appProperties.getMinio();
            MinioClient client = MinioClient.builder()
                    .endpoint(minio.getEndpoint())
                    .credentials(minio.getAccessKey(), minio.getSecretKey())
                    .build();

            String bucket = bucketName;

            int effectiveExpiry = expiresInSeconds > 0 ? expiresInSeconds : 900;

            return client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                        .bucket(bucket)
                            .object(objectPath)
                            .expiry(effectiveExpiry, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception exception) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DOWNLOAD_FAILED);
        }
    }

    /**
     * Hàm này sẽ đảm bảo rằng bucket đã tồn tại trong Minio, nếu bucket chưa tồn tại thì sẽ tự động tạo bucket đó. Hàm này sẽ sử dụng MinioClient để kiểm tra sự tồn tại của bucket và tạo bucket nếu cần thiết. Nếu có lỗi trong quá trình kiểm tra hoặc tạo bucket, hàm sẽ ném ra Exception để caller có thể xử lý.
     * @param client
     * @param bucket
     * @throws Exception
     */
    private void ensureBucket(MinioClient client, String bucket) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
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
