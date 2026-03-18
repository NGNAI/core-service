package ai.service;

import ai.AppProperties;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
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
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class MinioService {
    AppProperties appProperties;

    public String upload(MultipartFile file, String unit) {
        try {
            AppProperties.Minio minio = appProperties.getMinio();
            MinioClient client = MinioClient.builder()
                    .endpoint(minio.getEndpoint())
                    .credentials(minio.getAccessKey(), minio.getSecretKey())
                    .build();

            String bucket = minio.getBucket();
            ensureBucket(client, bucket);

            String objectPath = buildObjectPath(unit, file.getOriginalFilename());
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

    public MinioObjectData download(String objectPath) {
        try {
            AppProperties.Minio minio = appProperties.getMinio();
            MinioClient client = MinioClient.builder()
                    .endpoint(minio.getEndpoint())
                    .credentials(minio.getAccessKey(), minio.getSecretKey())
                    .build();

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

                return new MinioObjectData(outputStream.toByteArray(), "application/octet-stream");
            }
        } catch (Exception exception) {
            throw new AppException(ApiResponseStatus.MEDIA_UPLOAD_FAILED);
        }
    }

    private void ensureBucket(MinioClient client, String bucket) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String buildObjectPath(String unit, String originalFilename) {
        LocalDate now = LocalDate.now();
        String safeFileName = buildSafeFileName(originalFilename);
        return unit + "/" + now.getYear() + "/" + now.getMonthValue() + "/" + safeFileName;
    }

    private String buildSafeFileName(String originalFilename) {
        String source = (originalFilename == null || originalFilename.isBlank()) ? "file.bin" : originalFilename;
        String normalized = source.replace("\\", "_").replace("/", "_").replace(" ", "_");
        return UUID.randomUUID() + "-" + normalized;
    }

    @Data
    @AllArgsConstructor
    public static class MinioObjectData {
        byte[] bytes;
        String contentType;
    }
}
