package ai.service;

import java.util.Optional;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.AppProperties;
import ai.constant.CacheName;
import ai.dto.outer.ingestion.response.IngestionStatusResponseDto;
import ai.dto.outer.ingestion.response.IngestionUploadResponseDto;
import ai.dto.own.request.DataIngestionCreateFolderRequestDto;
import ai.dto.own.request.DataIngestionUpdateFolderRequestDto;
import ai.dto.own.request.DataIngestionUploadRequestDto;
import ai.dto.own.request.filter.DataIngestionFilterDto;
import ai.dto.own.response.DataIngestionDownloadData;
import ai.dto.own.response.DataIngestionJobStatusResponseDto;
import ai.dto.own.response.DataIngestionPresignedUrlResponseDto;
import ai.dto.own.response.DataIngestionResponseDto;
import ai.entity.postgres.DataIngestionEntity;
import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.DataIngestionDeleteStatus;
import ai.enums.DataSource;
import ai.enums.DataScope;
import ai.enums.IngestionStatus;
import ai.enums.SystemEventSource;
import ai.enums.SystemEventType;
import ai.exeption.AppException;
import ai.mapper.DataIngestionMapper;
import ai.repository.DataIngestionRepository;
import ai.util.JwtUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class DataIngestionService {
    // Định nghĩa thời gian mặc định cho presigned URL là 15 phút (900 giây), có thể cấu hình lại khi gọi API để lấy presigned URL với thời gian tùy chỉnh
    static int DEFAULT_PRESIGNED_EXPIRY_SECONDS = 900;
    static long DEFAULT_INGESTION_WAIT_TIMEOUT_MILLIS = 180_000;
    static long DEFAULT_INGESTION_POLL_INTERVAL_MILLIS = 2_000;

    DataIngestionRepository dataIngestionRepository;
    IngestionService ingestionService;
    MinioService minioService;
    DataIngestionMapper dataIngestionMapper;
    UserService userService;
    OrganizationService organizationService;
    SystemEventSseService systemEventSseService;
    AppProperties appProperties;

    /**
     * Định nghĩa phương thức để đồng bộ trạng thái ingestion mới nhất cho tất cả data ingestion đang ở trạng thái chưa hoàn thành (không phải COMPLETED hay FAILED), phương thức này sẽ được gọi định kỳ bởi scheduler để đảm bảo trạng thái ingestion luôn được cập nhật mới nhất, tránh trường hợp dữ liệu bị treo ở trạng thái intermediate mãi mãi do lỗi không nhận được callback từ ingestion service hoặc lỗi khi gọi API để lấy trạng thái ingestion. Các trạng thái được đồng bộ gồm: CREATED, EXTRACTING, CHUNKING, EMBEDDING, STORING
     */
    public void syncPendingIngestionStatuses() {
        System.out.println("Start syncing ingestion statuses for data ingestion items with non-final statuses...");
        // Lấy tất cả data ingestion có trạng thái ingestion không phải COMPLETED hay FAILED để đồng bộ trạng thái mới nhất từ ingestion service
        dataIngestionRepository.findByIngestionStatusNotFinal()
                .forEach(dataIngestion -> {
                    try {
                        pollIngestionJobStatus(dataIngestion.getId());
                    } catch (Exception exception) {
                        // Log lỗi và tiếp tục đồng bộ các item còn lại để không chặn toàn bộ tiến trình
                        System.err.println("Error syncing ingestion status for data ingestion with ID: " + dataIngestion.getId());
                        exception.printStackTrace();
                    }
                });
        System.out.println("Finished syncing ingestion statuses for data ingestion items with non-final statuses.");
    }

    /**
     * Định nghĩa phương thức để xử lý hàng đợi xóa các data ingestion đang ở trạng thái pending delete, phương thức này sẽ được gọi định kỳ bởi scheduler để đảm bảo các data ingestion cần xóa được xử lý kịp thời, tránh trường hợp dữ liệu bị treo ở trạng thái pending delete mãi mãi do lỗi khi xóa trên MinIO hoặc lỗi khi xóa trong database. Phương thức này sẽ tìm tất cả data ingestion có trạng thái delete là PENDING_DELETE, sau đó thực hiện xóa file trên MinIO nếu có và xóa bản ghi trong database, nếu có lỗi xảy ra trong quá trình xử lý một item nào đó thì sẽ log lỗi và tiếp tục xử lý các item còn lại để không chặn toàn bộ tiến trình
     */
    public void processPendingDeleteQueue() {
        System.out.println("Start processing pending data ingestion deletions...");
        dataIngestionRepository.findByDeleteStatus(DataIngestionDeleteStatus.PENDING_DELETE)
                .forEach(dataIngestion -> {
                    try {
                        executeDelete(dataIngestion);
                    } catch (Exception exception) {
                        // Log lỗi và tiếp tục xử lý các item còn lại
                        System.err.println("Error processing delete for data ingestion with ID: " + dataIngestion.getId());
                        exception.printStackTrace();
                    }
                });
        System.out.println("Finished processing pending data ingestion deletions.");
    }

    /**
     * Định nghĩa phương thức để upload file và tạo data ingestion mới, phương thức này sẽ được gọi khi người dùng upload file mới thông qua API, phương thức sẽ thực hiện các bước sau: 1) xác thực người dùng và tổ chức từ JWT token, 2) upload file lên MinIO trước để đảm bảo nếu có lỗi xảy ra khi upload file thì sẽ không tạo bản ghi data ingestion trong database, tránh trường hợp dữ liệu bị lỗi không thể retry được, 3) tạo bản ghi data ingestion mới trong database với thông tin về file đã upload và trạng thái ingestion là PENDING, 4) gọi API của ingestion service để đẩy file đã upload sang ingestion service để xử lý, nếu có lỗi xảy ra khi gọi API của ingestion service hoặc response trả về không hợp lệ thì sẽ cập nhật trạng thái ingestion của data ingestion này thành FAILED để tránh bị treo ở trạng thái PENDING mãi mãi, đồng thời trả về response cho client để client có thể hiển thị thông báo lỗi chính xác
     * @param requestDto
     * @return
     */
    @Transactional(noRollbackFor = AppException.class)
    public DataIngestionResponseDto uploadDataIngestion(DataIngestionUploadRequestDto requestDto, DataSource fromSource) {
        return uploadDataIngestion(requestDto, JwtUtil.getUserId(), JwtUtil.getOrgId(), fromSource);
    }

    /**
     * Định nghĩa phương thức để upload file và tạo data ingestion mới, phương thức này sẽ được gọi khi người dùng upload file mới thông qua API, phương thức sẽ thực hiện các bước sau: 1) xác thực người dùng và tổ chức từ tham số truyền vào, 2) upload file lên MinIO trước để đảm bảo nếu có lỗi xảy ra khi upload file thì sẽ không tạo bản ghi data ingestion trong database, tránh trường hợp dữ liệu bị lỗi không thể retry được, 3) tạo bản ghi data ingestion mới trong database với thông tin về file đã upload và trạng thái ingestion là PENDING, 4) gọi API của ingestion service để đẩy file đã upload sang ingestion service để xử lý, nếu có lỗi xảy ra khi gọi API của ingestion service hoặc response trả về không hợp lệ thì sẽ cập nhật trạng thái ingestion của data ingestion này thành FAILED để tránh bị treo ở trạng thái PENDING mãi mãi, đồng thời trả về response cho client để client có thể hiển thị thông báo lỗi chính xác
     * @param requestDto
     * @param userId
     * @param organizationId
     * @return
     */
    @Transactional(noRollbackFor = AppException.class)
    public DataIngestionResponseDto uploadDataIngestion(DataIngestionUploadRequestDto requestDto, UUID userId, UUID organizationId, DataSource fromSource) {
        UserEntity user = userService.getEntityById(userId);
        OrganizationEntity organization = organizationService.getEntityById(organizationId);

        // Push lên MinIO trước để tránh trường hợp đã lưu data ingestion vào database nhưng
        // upload file lên MinIO thất bại, dẫn đến dữ liệu bị lỗi không thể retry
        // ingestion được
        String minioPath = minioService.upload(requestDto.getFile(), user.getUserName(), organization.getName(), fromSource.name().toLowerCase());

        DataIngestionEntity dataIngestion = new DataIngestionEntity();
        dataIngestion.setName(requestDto.getFile().getOriginalFilename());
        dataIngestion.setFolder(false);
        dataIngestion.setMinioPath(minioPath);
        dataIngestion.setFileSize(requestDto.getFile().getSize());
        dataIngestion.setContentType(requestDto.getFile().getContentType());
        dataIngestion.setAccessLevel(requestDto.getAccessLevel());
        dataIngestion.setFromSource(fromSource);
        dataIngestion.setOwner(user);
        dataIngestion.setOrganization(organization);
        dataIngestion.setIngestionStatus(IngestionStatus.CREATED);
        dataIngestion.setDeleteStatus(DataIngestionDeleteStatus.ACTIVE);

        if (requestDto.getFolderId() != null) {
            DataIngestionEntity parent = dataIngestionRepository.findById(requestDto.getFolderId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_PARENT_NOT_EXISTS));
            if(parent.isFolder() == false){
                throw new AppException(ApiResponseStatus.DATA_INGESTION_PARENT_MUST_BE_FOLDER);
            }
            dataIngestion.setParent(parent);
        }

        dataIngestion = dataIngestionRepository.save(dataIngestion);

        try {
            String callbackUrl = resolveCallbackUrl(requestDto.getCallbackUrl());
            IngestionUploadResponseDto ingestionResponse = ingestionService.pushToVector(
                    requestDto.getFile(),
                    dataIngestion.getId().toString(),
                    user.getUserName(),
                    organization.getId().toString(),
                    organization.getName(),
                dataIngestion.getAccessLevel(),
                callbackUrl);

            // Nếu response từ ingestion service không hợp lệ thì đánh dấu data ingestion này là
            // failed để tránh trường hợp dữ liệu bị treo ở trạng thái pending mãi mãi, đồng
            // thời trả về lỗi cho client để client có thể hiển thị thông báo lỗi chính xác
            if (ingestionResponse == null || ingestionResponse.getJobId() == null) {
                dataIngestion.setIngestionStatus(IngestionStatus.FAILED);
                dataIngestionRepository.save(dataIngestion);

                return dataIngestionMapper.entityToResponseDto(dataIngestion);
            }

            // Cập nhật jobId và trạng thái ingestion sau khi đã đẩy sang
            // ingestion service thành công, khởi tạo status là CREATED
            dataIngestion.setJobId(ingestionResponse.getJobId());
            dataIngestion.setIngestionStatus(IngestionStatus.CREATED);
            dataIngestion = dataIngestionRepository.save(dataIngestion);

            return dataIngestionMapper.entityToResponseDto(dataIngestion);

        } catch (AppException exception) {
            exception.printStackTrace();
            dataIngestion.setIngestionStatus(IngestionStatus.FAILED);
            dataIngestionRepository.save(dataIngestion);
            return dataIngestionMapper.entityToResponseDto(dataIngestion);
        }
    }

    /**
     * Định nghĩa phương thức để tạo thư mục mới, phương thức này sẽ được gọi khi người dùng tạo thư mục mới thông qua API, phương thức sẽ thực hiện các bước sau: 1) xác thực người dùng và tổ chức từ JWT token, 2) tạo bản ghi data ingestion mới trong database với thông tin về thư mục và đánh dấu là folder, trạng thái ingestion sẽ để null vì thư mục không cần ingest, 3) nếu có cung cấp parentId thì sẽ gán thư mục cha cho thư mục mới tạo, nếu parentId không tồn tại hoặc không phải là folder thì sẽ trả về lỗi, tránh trường hợp dữ liệu bị lỗi không thể retry được
     * @param requestDto
     * @return
     */
    @Transactional
    public DataIngestionResponseDto createFolder(DataIngestionCreateFolderRequestDto requestDto, DataSource fromSource) {
        UserEntity user = userService.getEntityById(JwtUtil.getUserId());
        OrganizationEntity organization = organizationService.getEntityById(JwtUtil.getOrgId());

        DataIngestionEntity dataIngestion = new DataIngestionEntity();
        dataIngestion.setName(requestDto.getName().trim());
        dataIngestion.setFolder(true);
        dataIngestion.setContentType(null);
        dataIngestion.setFileSize(0L);
        dataIngestion.setAccessLevel(requestDto.getAccessLevel());
        dataIngestion.setFromSource(fromSource);
        dataIngestion.setOwner(user);
        dataIngestion.setOrganization(organization);
        dataIngestion.setJobId(null);
        dataIngestion.setIngestionStatus(null);
        dataIngestion.setDeleteStatus(DataIngestionDeleteStatus.ACTIVE);

        if (requestDto.getParentId() != null) {
            DataIngestionEntity parent = dataIngestionRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_PARENT_NOT_EXISTS));
            if(parent.isFolder() == false){
                throw new AppException(ApiResponseStatus.DATA_INGESTION_PARENT_MUST_BE_FOLDER);
            }
            dataIngestion.setParent(parent);
        }
        dataIngestion = dataIngestionRepository.save(dataIngestion);

        return dataIngestionMapper.entityToResponseDto(dataIngestion);
    }

    /**
     * Định nghĩa phương thức để ingest file đã có sẵn trên hệ thống, phương thức này sẽ được gọi bởi luồng xử lý tự động để ingest file từ thư mục đầu vào mà không cần phải upload lại, phương thức sẽ thực hiện các bước sau: 1) xác thực người dùng và tổ chức từ tham số truyền vào, 2) kiểm tra file đã tồn tại và hợp lệ hay chưa, nếu không hợp lệ thì trả về lỗi để luồng xử lý tự động có thể di chuyển file sang thư mục failed, 3) tạo bản ghi data ingestion mới trong database với thông tin về file và trạng thái ingestion là PENDING, 4) gọi API của ingestion service để đẩy file sang ingestion service để xử lý, nếu có lỗi xảy ra khi gọi API của ingestion service hoặc response trả về không hợp lệ thì sẽ cập nhật trạng thái ingestion của data ingestion này thành FAILED để tránh bị treo ở trạng thái PENDING mãi mãi, đồng thời trả về response để luồng xử lý tự động có thể di chuyển file sang thư mục failed
     * @param stagedFile
     * @param relativePath
     * @param ownerId
     * @param organizationId
     * @param accessLevel
     * @return
     */
    @Transactional(noRollbackFor = AppException.class)
    public DataIngestionResponseDto ingestLocalFile(
            Path stagedFile,
            Path relativePath,
            UUID ownerId,
            UUID organizationId,
            DataScope accessLevel,
            DataSource fromSource) {
        if (stagedFile == null || !Files.exists(stagedFile) || !Files.isRegularFile(stagedFile)) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FILE_REQUIRED);
        }

        UserEntity owner = userService.getEntityById(ownerId);
        OrganizationEntity organization = organizationService.getEntityById(organizationId);

        DataIngestionEntity parent = resolveOrCreateFolderTree(
                relativePath == null ? null : relativePath.getParent(),
                owner,
                organization,
            accessLevel,
            fromSource);

        String fileName = stagedFile.getFileName().toString();
        String contentType;
        long fileSize;
        byte[] fileBytes;

        try {
            contentType = Files.probeContentType(stagedFile);
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }
            fileBytes = Files.readAllBytes(stagedFile);
            fileSize = fileBytes.length;
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new AppException(ApiResponseStatus.DATA_INGESTION_UPLOAD_FAILED);
        }

        String minioPath = minioService.upload(fileBytes, fileName, contentType, owner.getUserName(), organization.getName(), fromSource.name().toLowerCase());

        DataIngestionEntity dataIngestion = new DataIngestionEntity();
        dataIngestion.setName(fileName);
        dataIngestion.setFolder(false);
        dataIngestion.setMinioPath(minioPath);
        dataIngestion.setFileSize(fileSize);
        dataIngestion.setContentType(contentType);
        dataIngestion.setAccessLevel(accessLevel);
        dataIngestion.setFromSource(fromSource);
        dataIngestion.setOwner(owner);
        dataIngestion.setOrganization(organization);
        dataIngestion.setIngestionStatus(IngestionStatus.CREATED);
        dataIngestion.setDeleteStatus(DataIngestionDeleteStatus.ACTIVE);
        dataIngestion.setParent(parent);

        dataIngestion = dataIngestionRepository.save(dataIngestion);

        try {
            String callbackUrl = resolveCallbackUrl(null);
            IngestionUploadResponseDto ingestionResponse = ingestionService.pushToVector(
                    fileBytes,
                    fileName,
                    dataIngestion.getId().toString(),
                    owner.getUserName(),
                    organization.getId().toString(),
                    organization.getName(),
                accessLevel,
                callbackUrl);

            if (ingestionResponse == null || ingestionResponse.getJobId() == null) {
                dataIngestion.setIngestionStatus(IngestionStatus.FAILED);
                dataIngestion = dataIngestionRepository.save(dataIngestion);
                return dataIngestionMapper.entityToResponseDto(dataIngestion);
            }

            dataIngestion.setJobId(ingestionResponse.getJobId());
            dataIngestion.setIngestionStatus(IngestionStatus.CREATED);
            dataIngestion = dataIngestionRepository.save(dataIngestion);
            return dataIngestionMapper.entityToResponseDto(dataIngestion);
        } catch (AppException exception) {
            exception.printStackTrace();
            dataIngestion.setIngestionStatus(IngestionStatus.FAILED);
            dataIngestion = dataIngestionRepository.save(dataIngestion);
            return dataIngestionMapper.entityToResponseDto(dataIngestion);
        }
    }

    /**
     * Định nghĩa phương thức để lấy chi tiết data ingestion theo ID, phương thức này sẽ được gọi khi người dùng xem chi tiết một data ingestion cụ thể, phương thức sẽ thực hiện các bước sau: 1) kiểm tra cache trước để lấy thông tin data ingestion nếu đã từng truy cập trước đó, tránh phải truy vấn database nhiều lần cho cùng một data ingestion, 2) nếu không có trong cache thì truy vấn database để lấy thông tin data ingestion, nếu không tồn tại thì trả về lỗi, 3) ánh xạ entity sang response DTO và trả về cho client, đồng thời lưu vào cache để lần sau truy cập nhanh hơn. Cache sẽ được tự động làm mới khi có cập nhật liên quan đến data ingestion này (ví dụ: đổi tên thư mục, di chuyển thư mục, xóa data ingestion)
     * @param dataIngestionId
     * @return
     */
    @Cacheable(value = CacheName.DATA_INGESTION_DTO_DETAILS, key = "#dataIngestionId", unless = "#result == null", condition = "#dataIngestionId != null")
    @Transactional(readOnly = true)
    public DataIngestionEntity getEntityById(UUID dataIngestionId) {
        return dataIngestionRepository.findById(dataIngestionId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));
    }

    /**
     * Định nghĩa phương thức để lấy chi tiết data ingestion theo ID, phương thức này sẽ được gọi khi người dùng xem chi tiết một data ingestion cụ thể, phương thức sẽ thực hiện các bước sau: 1) truy vấn database để lấy thông tin data ingestion, nếu không tồn tại thì trả về lỗi, 2) ánh xạ entity sang response DTO và trả về cho client. Phương thức này sẽ không cache kết quả vì thường được gọi sau khi đã gọi getEntityById để lấy entity và thực hiện các kiểm tra liên quan đến trạng thái của data ingestion, nếu cache kết quả của phương thức này có thể dẫn đến việc trả về dữ liệu cũ không phản ánh đúng trạng thái hiện tại của data ingestion sau khi đã có sự thay đổi (ví dụ: đổi tên thư mục, di chuyển thư mục, xóa data ingestion)
     * @param dataIngestionId
     * @return
     */
    public DataIngestionResponseDto getById(UUID dataIngestionId) {
        DataIngestionEntity dataIngestion = dataIngestionRepository.findById(dataIngestionId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));
        return dataIngestionMapper.entityToResponseDto(dataIngestion);
    }

    /**
     * Định nghĩa phương thức để lấy chi tiết data ingestion đã hoàn thành theo ID, phương thức này sẽ được gọi khi người dùng muốn tải file của một data ingestion cụ thể, phương thức sẽ thực hiện các bước sau: 1) truy vấn database để lấy thông tin data ingestion, nếu không tồn tại thì trả về lỗi, 2) kiểm tra xem data ingestion này có hợp lệ để tải xuống hay không (ví dụ: không phải là folder, trạng thái ingestion đã thành công, không đang ở trạng thái pending delete), nếu không hợp lệ thì trả về lỗi, 3) nếu data ingestion còn ở trạng thái xử lý (CREATED, EXTRACTING, CHUNKING, EMBEDDING, STORING) thì sẽ gọi API của ingestion service để lấy trạng thái mới nhất và cập nhật vào database, sau đó kiểm tra lại trạng thái ingestion, nếu vẫn chưa hoàn thành thì trả về lỗi, 4) nếu đã hoàn thành thì trả về entity để phục vụ cho việc tải file sau đó. Phương thức này sẽ không cache kết quả vì thường được gọi trước khi tải file và cần đảm bảo luôn phản ánh đúng trạng thái hiện tại của data ingestion sau khi đã có sự thay đổi (ví dụ: đổi tên thư mục, di chuyển thư mục, xóa data ingestion)
     * @param dataIngestionId
     * @return
     */
    @Transactional
    public DataIngestionEntity getCompletedFileEntityById(UUID dataIngestionId) {
        DataIngestionEntity dataIngestion = getDataIngestionEntity(dataIngestionId);

        if (dataIngestion.isFolder()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_ONLY_OPERATION);
        }

        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(dataIngestion))) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DELETE_IN_PROGRESS);
        }

        // Nếu ingestion vẫn đang ở trạng thái intermediate (CREATED, EXTRACTING, CHUNKING, EMBEDDING, STORING)
        // thì cần poll trạng thái mới nhất từ ingestion service
        if (dataIngestion.getJobId() != null
                && dataIngestion.getIngestionStatus() != null
                && !IngestionStatus.COMPLETED.equals(dataIngestion.getIngestionStatus())
                && !IngestionStatus.FAILED.equals(dataIngestion.getIngestionStatus())) {
            pollIngestionJobStatus(dataIngestionId);
            dataIngestion = getDataIngestionEntity(dataIngestionId);
        }

        if (!IngestionStatus.COMPLETED.equals(dataIngestion.getIngestionStatus())) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_NOT_COMPLETED);
        }

        return dataIngestion;
    }

    /**
     * Định nghĩa phương thức để đợi cho đến khi trạng thái ingestion của data ingestion được cập nhật thành COMPLETED, phương thức này sẽ được gọi khi người dùng muốn tải file của một data ingestion cụ thể nhưng trạng thái ingestion hiện tại vẫn đang ở trạng thái intermediate (CREATED, EXTRACTING, CHUNKING, EMBEDDING, STORING), phương thức sẽ thực hiện các bước sau: 1) truy vấn database để lấy thông tin data ingestion, nếu không tồn tại thì trả về lỗi, 2) kiểm tra xem data ingestion này có hợp lệ để tải xuống hay không (ví dụ: không phải là folder, không đang ở trạng thái pending delete), nếu không hợp lệ thì trả về lỗi, 3) nếu data ingestion đang ở trạng thái intermediate thì sẽ gọi API của ingestion service để lấy trạng thái mới nhất và cập nhật vào database, sau đó kiểm tra lại trạng thái ingestion, nếu đã hoàn thành thì trả về entity để phục vụ cho việc tải file sau đó, nếu vẫn chưa hoàn thành thì tiếp tục đợi và kiểm tra lại cho đến khi hết thời gian chờ tối đa đã định nghĩa sẵn, 4) nếu hết thời gian chờ mà vẫn chưa hoàn thành thì trả về lỗi. Phương thức này sẽ không cache kết quả vì thường được gọi trước khi tải file và cần đảm bảo luôn phản ánh đúng trạng thái hiện tại của data ingestion sau khi đã có sự thay đổi (ví dụ: đổi tên thư mục, di chuyển thư mục, xóa data ingestion)
     * @param dataIngestionId
     * @return
     */
    public DataIngestionEntity waitForIngestionCompleted(UUID dataIngestionId) {
        long deadline = System.currentTimeMillis() + DEFAULT_INGESTION_WAIT_TIMEOUT_MILLIS;

        while (System.currentTimeMillis() <= deadline) {
            DataIngestionEntity dataIngestion = getDataIngestionEntity(dataIngestionId);

            if (dataIngestion.isFolder()) {
                throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_ONLY_OPERATION);
            }

            if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(dataIngestion))) {
                throw new AppException(ApiResponseStatus.DATA_INGESTION_DELETE_IN_PROGRESS);
            }

            if (IngestionStatus.COMPLETED.equals(dataIngestion.getIngestionStatus())) {
                return dataIngestion;
            }

            if (IngestionStatus.FAILED.equals(dataIngestion.getIngestionStatus())) {
                throw new AppException(ApiResponseStatus.DATA_INGESTION_NOT_COMPLETED);
            }

            if (dataIngestion.getJobId() == null) {
                throw new AppException(ApiResponseStatus.DATA_INGESTION_JOB_ID_NOT_EXISTS);
            }

            // Nếu status vẫn ở trạng thái intermediate (CREATED, EXTRACTING, CHUNKING, EMBEDDING, STORING)
            // thì poll trạng thái mới nhất từ ingestion service
            if (dataIngestion.getIngestionStatus() != null
                    && !IngestionStatus.COMPLETED.equals(dataIngestion.getIngestionStatus())
                    && !IngestionStatus.FAILED.equals(dataIngestion.getIngestionStatus())) {
                pollIngestionJobStatus(dataIngestionId);
            }

            try {
                Thread.sleep(DEFAULT_INGESTION_POLL_INTERVAL_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AppException(ApiResponseStatus.DATA_INGESTION_NOT_COMPLETED);
            }
        }

        throw new AppException(ApiResponseStatus.DATA_INGESTION_NOT_COMPLETED);
    }

    /**
     * Định nghĩa phương thức để tải file của data ingestion theo ID, phương thức này sẽ được gọi khi người dùng tải file của một data ingestion cụ thể, phương thức sẽ thực hiện các bước sau: 1) truy vấn database để lấy thông tin data ingestion, nếu không tồn tại thì trả về lỗi, 2) kiểm tra xem data ingestion này có hợp lệ để tải xuống hay không (ví dụ: không phải là folder, trạng thái ingestion đã thành công, không đang ở trạng thái pending delete), nếu không hợp lệ thì trả về lỗi, 3) gọi MinioService để tải file từ MinIO theo object path đã lưu trong data ingestion, nếu có lỗi xảy ra khi tải file thì trả về lỗi, 4) trả về dữ liệu file dưới dạng byte array cùng với content type để client có thể xử lý tải xuống. Phương thức này sẽ không cache dữ liệu file vì dữ liệu file có thể rất lớn và không nên lưu trong cache
     * @param dataIngestionId
     * @return
     */
    @Transactional
    public DataIngestionDownloadData downloadById(UUID dataIngestionId) {
        DataIngestionEntity dataIngestion = dataIngestionRepository.findById(dataIngestionId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));

        validateDownloadableDataIngestion(dataIngestion);

            MinioService.MinioObjectData objectData = minioService.download(dataIngestion.getMinioPath(), dataIngestion.getFromSource().name().toLowerCase());
        dataIngestionRepository.save(dataIngestion);

        return new DataIngestionDownloadData(
            dataIngestion.getName(),
                objectData.getContentType(),
                objectData.getBytes());
    }

    /**
     * Định nghĩa phương thức để lấy presigned URL để tải file của data ingestion theo ID, phương thức này sẽ được gọi khi người dùng muốn tải file của một data ingestion cụ thể nhưng không muốn tải trực tiếp qua server mà muốn tải trực tiếp từ MinIO thông qua presigned URL, phương thức sẽ thực hiện các bước sau: 1) truy vấn database để lấy thông tin data ingestion, nếu không tồn tại thì trả về lỗi, 2) kiểm tra xem data ingestion này có hợp lệ để tải xuống hay không (ví dụ: không phải là folder, trạng thái ingestion đã thành công, không đang ở trạng thái pending delete), nếu không hợp lệ thì trả về lỗi, 3) gọi MinioService để tạo presigned URL cho object path đã lưu trong data ingestion với thời gian hết hạn được cấu hình hoặc mặc định, nếu có lỗi xảy ra khi tạo presigned URL thì trả về lỗi, 4) trả về presigned URL cùng với thời gian hết hạn cho client. Phương thức này sẽ không cache presigned URL vì mỗi lần tạo presigned URL sẽ có một giá trị khác nhau và thời gian hết hạn cũng có thể khác nhau
     * @param dataIngestionId
     * @param expiresInSeconds
     * @return
     */
    @Transactional(readOnly = true)
    public DataIngestionPresignedUrlResponseDto getPresignedDownloadUrl(UUID dataIngestionId, Integer expiresInSeconds) {
        DataIngestionEntity dataIngestion = dataIngestionRepository.findById(dataIngestionId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));

        validateDownloadableDataIngestion(dataIngestion);

        int effectiveExpiry = expiresInSeconds == null || expiresInSeconds <= 0
                ? DEFAULT_PRESIGNED_EXPIRY_SECONDS
                : expiresInSeconds;

        String url = minioService.generatePresignedDownloadUrl(dataIngestion.getMinioPath(), effectiveExpiry, dataIngestion.getFromSource().name().toLowerCase());
        return DataIngestionPresignedUrlResponseDto.builder()
                .url(url)
                .expiresInSeconds(effectiveExpiry)
                .build();
    }

    /**
     * Định nghĩa phương thức để cập nhật thông tin thư mục, phương thức này sẽ được gọi khi người dùng muốn cập nhật thông tin của một thư mục cụ thể (ví dụ: đổi tên thư mục, di chuyển thư mục sang vị trí khác), phương thức sẽ thực hiện các bước sau: 1) truy vấn database để lấy thông tin data ingestion, nếu không tồn tại thì trả về lỗi, 2) kiểm tra xem data ingestion này có phải là folder hay không, nếu không phải là folder thì trả về lỗi vì chỉ cho phép cập nhật thông tin cho thư mục, 3) kiểm tra các thông tin cần cập nhật có hợp lệ hay không (ví dụ: tên thư mục không được để trống, nếu có parentId mới thì parentId đó phải tồn tại và phải là folder), nếu không hợp lệ thì trả về lỗi, 4) cập nhật thông tin cho thư mục trong database, đồng thời xóa cache chi tiết của data ingestion này để lần sau truy cập sẽ lấy thông tin mới nhất từ database. Phương thức này chỉ cho phép cập nhật thông tin của thư mục mà không cho phép cập nhật file của data ingestion vì việc cập nhật file sẽ phức tạp hơn nhiều (ví dụ: cần phải upload lại file lên MinIO, gọi lại API của ingestion service để đẩy file mới sang ingestion service, xử lý trạng thái ingestion phức tạp hơn) nên sẽ không hỗ trợ trong phương
     * @param dataIngestionId
     * @param requestDto
     * @return
     */
    @CacheEvict(value = CacheName.DATA_INGESTION_DTO_DETAILS, key = "#dataIngestionId", condition = "#dataIngestionId != null")
    @Transactional
    public DataIngestionResponseDto updateFolder(UUID dataIngestionId, DataIngestionUpdateFolderRequestDto requestDto) {
        DataIngestionEntity folder = dataIngestionRepository.findById(dataIngestionId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));

        if (!folder.isFolder()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_ONLY_OPERATION);
        }

        if(requestDto == null){
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_UPDATE_REQUIRED);
        }

        boolean hasName = requestDto.getName() != null && !requestDto.getName().isBlank();
        boolean hasParent = requestDto.getParentId() != null;
        boolean moveToRoot = Boolean.TRUE.equals(requestDto.getMoveToRoot());
        if (!hasName && !hasParent && !moveToRoot) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_UPDATE_REQUIRED);
        }

        // Đổi tên
        if (hasName) {
            folder.setName(requestDto.getName().trim());
        }

        // Di chuyển thư mục
        if (moveToRoot) {
            folder.setParent(null);
        } else if (hasParent) {
            DataIngestionEntity parent = dataIngestionRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_PARENT_NOT_EXISTS));

            if (!parent.isFolder()) {
                throw new AppException(ApiResponseStatus.DATA_INGESTION_PARENT_MUST_BE_FOLDER);
            }
            if (folder.getId().equals(parent.getId())) {
                throw new AppException(ApiResponseStatus.DATA_INGESTION_MOVE_CYCLE_NOT_ALLOWED);
            }
            if (isDescendantOf(parent, folder)) {
                throw new AppException(ApiResponseStatus.DATA_INGESTION_MOVE_CYCLE_NOT_ALLOWED);
            }

            folder.setParent(parent);
        }

        return dataIngestionMapper.entityToResponseDto(dataIngestionRepository.save(folder));
    }

    /**
     * Định nghĩa phương thức để lấy danh sách data ingestion với phân trang và lọc theo các tiêu chí khác nhau, phương thức này sẽ được gọi khi người dùng xem danh sách data ingestion, phương thức sẽ thực hiện các bước sau: 1) xác thực người dùng và tổ chức từ JWT token, 2) xây dựng Specification để lọc dữ liệu trong database dựa trên các tiêu chí truyền vào (ví dụ: lọc theo tên, lọc theo thư mục cha, lọc theo trạng thái ingestion), đồng thời chỉ lấy những data ingestion thuộc về tổ chức của người dùng và do chính người dùng đó sở hữu để đảm bảo tính bảo mật và phân quyền, 3) gọi repository để truy vấn database với Specification đã xây dựng và pageable được tạo từ thông tin phân trang truyền vào, 4) ánh xạ kết quả trả về sang response DTO và trả về cho client. Phương thức này sẽ không cache kết quả vì kết quả có thể thay đổi thường xuyên khi có cập nhật liên quan đến data ingestion
     * @param filterDto
     * @return
     */
    @Transactional(readOnly = true)
    public Page<DataIngestionResponseDto> getAll(DataIngestionFilterDto filterDto) {

        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        Specification<DataIngestionEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) -> {
            Predicate orgIdPredicate = criteriaBuilder.equal(root.get("organization").get("id"), orgId);
            Predicate ownerPredicate = criteriaBuilder.equal(root.get("owner").get("id"), userId);
            return criteriaBuilder.and(orgIdPredicate, ownerPredicate);
        });
        return dataIngestionRepository.findAll(spec, filterDto.createPageable())
                .map(dataIngestionMapper::entityToResponseDto);
    }

    /**
     * Định nghĩa phương thức để retry một data ingestion đã bị lỗi, phương thức này sẽ được gọi khi người dùng muốn thử lại quá trình ingest cho một data ingestion cụ thể đã ở trạng thái FAILED, phương thức sẽ thực hiện các bước sau: 1) truy vấn database để lấy thông tin data ingestion theo ID, nếu không tồn tại thì trả về lỗi, 2) kiểm tra xem data ingestion này có hợp lệ để retry hay không (ví dụ: không phải là folder, đang ở trạng thái FAILED, không đang ở trạng thái pending delete), nếu không hợp lệ thì trả về lỗi, 3) gọi MinioService để tải file từ MinIO theo object path đã lưu trong data ingestion, nếu có lỗi xảy ra khi tải file thì trả về lỗi, 4) gọi API của ingestion service để đẩy file sang ingestion service để xử lý lại, nếu có lỗi xảy ra khi gọi API của ingestion service hoặc response trả về không hợp lệ thì sẽ cập nhật trạng thái ingestion của data ingestion này thành FAILED để tránh bị treo ở trạng thái PENDING mãi mãi, đồng thời trả về response cho client để client có thể hiển thị thông báo lỗi chính xác. Phương thức này sẽ không cache kết quả vì kết quả có thể thay đổi sau khi retry
     * @param dataIngestionId
     * @return
     */
    @Transactional(noRollbackFor = AppException.class)
    public DataIngestionResponseDto retryIngestion(UUID dataIngestionId) {
        DataIngestionEntity dataIngestion = dataIngestionRepository.findById(dataIngestionId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));

        // Không cho phép nếu là folder
        if (dataIngestion.isFolder()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_ONLY_OPERATION);
        }

        // Không cho phép retry ingestion với data ingestion đang ở trạng thái pending delete
        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(dataIngestion))) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DELETE_IN_PROGRESS);
        }

        // Chỉ cho phép retry nếu trạng thái ingestion hiện tại là FAILED
        if (dataIngestion.getIngestionStatus() == null || !IngestionStatus.FAILED.equals(dataIngestion.getIngestionStatus())) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_RETRY_ONLY_FAILED);
        }

        // Kiểm tra minioPath hợp lệ trước khi retry
        if (dataIngestion.getMinioPath() == null || dataIngestion.getMinioPath().isBlank()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_UPLOAD_FAILED);
        }

        UserEntity owner = dataIngestion.getOwner();
        OrganizationEntity organization = dataIngestion.getOrganization();

        try {
            MinioService.MinioObjectData objectData = minioService.download(dataIngestion.getMinioPath(), dataIngestion.getFromSource().name().toLowerCase());
            String callbackUrl = resolveCallbackUrl(null);
            IngestionUploadResponseDto ingestionResponse = ingestionService.pushToVector(
                    objectData.getBytes(),
                    dataIngestion.getName(),
                    dataIngestion.getId().toString(),
                    owner.getUserName(),
                    organization.getId().toString(),
                    organization.getName(),
                dataIngestion.getAccessLevel(),
                callbackUrl);
            System.out.println("Ingestion response after retrying ingestion for data ingestion with ID " + dataIngestionId + ": " + ingestionResponse);

             // Nếu response từ ingestion service không hợp lệ thì đánh dấu dữ liệu này là failed để tránh bị treo ở trạng thái pending mãi mãi

            if (ingestionResponse == null || ingestionResponse.getJobId() == null) {
                dataIngestion.setIngestionStatus(IngestionStatus.FAILED);
                dataIngestionRepository.save(dataIngestion);
                throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
            }

            dataIngestion.setJobId(ingestionResponse.getJobId());
            dataIngestion.setIngestionStatus(IngestionStatus.CREATED);
            dataIngestionRepository.save(dataIngestion);

            return dataIngestionMapper.entityToResponseDto(dataIngestion);
        } catch (AppException exception) {
            exception.printStackTrace();
            dataIngestion.setIngestionStatus(IngestionStatus.FAILED);
            dataIngestionRepository.save(dataIngestion);
            throw exception;
        }
    }

    /**
     * Định nghĩa phương thức để poll trạng thái ingestion mới nhất cho một data ingestion cụ thể, phương thức này sẽ được gọi khi người dùng muốn xem trạng thái ingestion mới nhất cho một data ingestion cụ thể, phương thức sẽ thực hiện các bước sau: 1) truy vấn database để lấy thông tin data ingestion theo ID, nếu không tồn tại thì trả về lỗi, 2) kiểm tra xem data ingestion này có hợp lệ để poll trạng thái hay không (ví dụ: không phải là folder, không đang ở trạng thái pending delete), nếu không hợp lệ thì trả về lỗi, 3) gọi API của ingestion service để lấy trạng thái ingestion mới nhất dựa trên jobId đã lưu trong data ingestion, nếu có lỗi xảy ra khi gọi API của ingestion service hoặc response trả về không hợp lệ thì sẽ trả về lỗi, 4) cập nhật trạng thái ingestion mới nhất vào database và trả về cho client. Phương thức này sẽ không cache kết quả vì trạng thái ingestion có thể thay đổi thường xuyên và cần được cập nhật mới nhất mỗi khi người dùng yêu cầu
     * @param dataIngestionId
     * @return
     */
    @CacheEvict(value = CacheName.DATA_INGESTION_DTO_DETAILS, key = "#dataIngestionId", condition = "#dataIngestionId != null")
    @Transactional
    public DataIngestionJobStatusResponseDto pollIngestionJobStatus(UUID dataIngestionId) {
        Optional<DataIngestionEntity> dataIngestionOptional = dataIngestionRepository.findById(dataIngestionId);
        if (dataIngestionOptional.isEmpty()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS);
        }

        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(dataIngestionOptional.get()))) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DELETE_IN_PROGRESS);
        }
        
        if(dataIngestionOptional.get().getJobId() == null) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_JOB_ID_NOT_EXISTS);
        }

        IngestionStatusResponseDto ingestionStatusResponse = ingestionService.getJobStatus(dataIngestionOptional.get().getJobId());
        return updateStatusAndBuildResponse(dataIngestionOptional.get(), ingestionStatusResponse, true);
    }

    @CacheEvict(value = CacheName.DATA_INGESTION_DTO_DETAILS, key = "#result.dataIngestionId", condition = "#result != null && #result.dataIngestionId != null")
    @Transactional
    public DataIngestionJobStatusResponseDto handleIngestionCallback(IngestionStatusResponseDto callbackDto) {
        if (callbackDto == null) {
            throw new AppException(ApiResponseStatus.INVALID_REQUEST_INFORMATION);
        }

        DataIngestionEntity dataIngestion;
        if (callbackDto.getMeta() != null) {
            dataIngestion = dataIngestionRepository.findById(callbackDto.getMeta().getFile_id())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));
        } else if (callbackDto.getJobId() != null) {
            dataIngestion = dataIngestionRepository.findByJobId(callbackDto.getJobId())
                    .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));
        } else {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_JOB_ID_NOT_EXISTS);
        }

        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(dataIngestion))) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DELETE_IN_PROGRESS);
        }

        return updateStatusAndBuildResponse(dataIngestion, callbackDto, true);
    }

    /**
     * Định nghĩa phương thức để xóa một data ingestion theo ID, phương thức này sẽ được gọi khi người dùng muốn xóa một data ingestion cụ thể, phương thức sẽ thực hiện các bước sau: 1) truy vấn database để lấy thông tin data ingestion theo ID, nếu không tồn tại thì trả về lỗi, 2) kiểm tra xem data ingestion này có hợp lệ để xóa hay không (ví dụ: nếu là folder thì không cho phép xóa bằng API file mà phải gọi API xóa folder, nếu đang ở trạng thái pending delete thì không cho phép xóa nữa), nếu không hợp lệ thì trả về lỗi, 3) cập nhật trạng thái delete của data ingestion thành PENDING_DELETE để đánh dấu là đang chờ xóa, sau đó trả về thông tin data ingestion đã được cập nhật cho client. Thực tế việc xóa sẽ được thực hiện bởi một tiến trình riêng biệt định kỳ gọi phương thức processPendingDeleteQueue để xử lý các data ingestion đang ở trạng thái pending delete nhằm đảm bảo việc xóa được thực hiện kịp thời và tránh trường hợp dữ liệu bị treo ở trạng thái pending delete mãi mãi do lỗi khi xóa trên MinIO hoặc lỗi khi xóa trong database. Phương thức này sẽ xóa cache chi tiết của data ingestion này để lần sau truy cập sẽ lấy thông tin mới nhất từ database
     * @param dataIngestionId
     * @return
     */
    @CacheEvict(value = CacheName.DATA_INGESTION_DTO_DETAILS, key = "#dataIngestionId", condition = "#dataIngestionId != null")
    @Transactional
    public DataIngestionResponseDto deleteFileById(UUID dataIngestionId) {
        DataIngestionEntity dataIngestion = dataIngestionRepository.findById(dataIngestionId).orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));

        // Nếu data ingestion là folder thì không cho phép xóa bằng API file
        if (dataIngestion.isFolder()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_ONLY_OPERATION);
        }

        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(dataIngestion))) {
            return dataIngestionMapper.entityToResponseDto(dataIngestion);
        }

        dataIngestion.setDeleteStatus(DataIngestionDeleteStatus.PENDING_DELETE);
        dataIngestion = dataIngestionRepository.save(dataIngestion);

        return dataIngestionMapper.entityToResponseDto(dataIngestion);
    }

    /**
     * Định nghĩa phương thức để xóa một thư mục data ingestion theo ID, phương thức này sẽ được gọi khi người dùng muốn xóa một thư mục data ingestion cụ thể, phương thức sẽ thực hiện các bước sau: 1) truy vấn database để lấy thông tin data ingestion theo ID, nếu không tồn tại thì trả về lỗi, 2) kiểm tra xem data ingestion này có phải là folder hay không, nếu không phải là folder thì trả về lỗi vì chỉ cho phép xóa thư mục bằng API này, 3) xóa trực tiếp thư mục này trong database vì việc xóa thư mục sẽ tự động cascade xóa tất cả các data ingestion con bên dưới nó, đồng thời xóa cache chi tiết của data ingestion này để lần sau truy cập sẽ lấy thông tin mới nhất từ database. Phương thức này sẽ không đánh dấu trạng thái delete thành PENDING_DELETE như phương thức xóa file mà sẽ xóa trực tiếp vì việc xóa thư mục thường ít gặp lỗi hơn so với việc xóa file (ví dụ: không cần phải xóa trên MinIO) nên có thể thực hiện trực tiếp để tránh trường hợp dữ liệu bị treo ở trạng thái pending delete mãi mãi do lỗi khi xóa trên MinIO hoặc lỗi khi xóa trong database
     * @param dataIngestionId
     * @return
     */
    @CacheEvict(value = CacheName.DATA_INGESTION_DTO_DETAILS, key = "#dataIngestionId", condition = "#dataIngestionId != null")
    @Transactional
    public DataIngestionResponseDto deleteById(UUID dataIngestionId) {
        return deleteFileById(dataIngestionId);
    }

    /**
     * Định nghĩa phương thức để xử lý hàng đợi xóa các data ingestion đang ở trạng thái pending delete, phương thức này sẽ được gọi định kỳ bởi một tiến trình riêng biệt để đảm bảo việc xóa được thực hiện kịp thời và tránh trường hợp dữ liệu bị treo ở trạng thái pending delete mãi mãi do lỗi khi xóa trên MinIO hoặc lỗi khi xóa trong database, phương thức sẽ thực hiện các bước sau: 1) truy vấn database để lấy danh sách các data ingestion đang ở trạng thái pending delete, 2) với mỗi data ingestion trong danh sách, thực hiện xóa trên MinIO nếu có minioPath, sau đó xóa bản ghi data ingestion trong database, nếu có lỗi xảy ra khi xóa trên MinIO hoặc lỗi khi xóa trong database thì sẽ cập nhật trạng thái delete của data ingestion đó thành DELETE_FAILED để đánh dấu là đã có lỗi xảy ra khi xóa và cần phải retry lại, tránh trường hợp dữ liệu bị treo ở trạng thái pending delete mãi mãi do lỗi khi xóa trên MinIO hoặc lỗi khi xóa trong database
     * @param dataIngestion
     */
    private void executeDelete(DataIngestionEntity dataIngestion) {
        try {
            // Xóa bên minio trước để tránh rác file nếu xóa database thành công nhưng xóa object thất bại
            if (dataIngestion.getMinioPath() != null && !dataIngestion.getMinioPath().isBlank()) {
                minioService.delete(dataIngestion.getMinioPath(), dataIngestion.getFromSource().name().toLowerCase());
            }

            // Nếu data ingestion này liên quan đến ingestion job nào đó thì gọi API của ingestion service để xóa job đó luôn, tránh trường hợp dữ liệu bị xóa nhưng job
            if (dataIngestion.getJobId() != null) {
                ingestionService.deleteFile(dataIngestion.getId().toString());
            }

            dataIngestionRepository.delete(dataIngestion);
        } catch (Exception exception) {
            dataIngestionRepository.findById(dataIngestion.getId()).ifPresent(entity -> {
                entity.setDeleteStatus(DataIngestionDeleteStatus.DELETE_FAILED);
                dataIngestionRepository.save(entity);
            });
            throw exception;
        }
    }

    /**
     * Định nghĩa phương thức để xóa một thư mục data ingestion theo ID, phương thức này sẽ được gọi khi người dùng muốn xóa một thư mục data ingestion cụ thể, phương thức sẽ thực hiện các bước sau: 1) truy vấn database để lấy thông tin data ingestion theo ID, nếu không tồn tại thì trả về lỗi, 2) kiểm tra xem data ingestion này có phải là folder hay không, nếu không phải là folder thì trả về lỗi vì chỉ cho phép xóa thư mục bằng API này, 3) xóa trực tiếp thư mục này trong database vì việc xóa thư mục sẽ tự động cascade xóa tất cả các data ingestion con bên dưới nó, đồng thời xóa cache chi tiết của data ingestion này để lần sau truy cập sẽ lấy thông tin mới nhất từ database. Phương thức này sẽ không đánh dấu trạng thái delete thành PENDING_DELETE như phương thức xóa file mà sẽ xóa trực tiếp vì việc xóa thư mục thường ít gặp lỗi hơn so với việc xóa file (ví dụ: không cần phải xóa trên MinIO) nên có thể thực hiện trực tiếp để tránh trường hợp dữ liệu bị treo ở trạng thái pending delete mãi mãi do lỗi khi xóa trên MinIO hoặc lỗi khi xóa trong database
     * @param dataIngestionId
     */
    @CacheEvict(value = CacheName.DATA_INGESTION_DTO_DETAILS, key = "#dataIngestionId", condition = "#dataIngestionId != null")
    @Transactional
    public void deleteFolderById(UUID dataIngestionId) {
        DataIngestionEntity dataIngestion = dataIngestionRepository.findById(dataIngestionId).orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));
        if (!dataIngestion.isFolder()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_ONLY_OPERATION);
        }
        
        dataIngestionRepository.delete(dataIngestion);
    }

    /**
     * Định nghĩa phương thức để kiểm tra xem một data ingestion có hợp lệ để tải xuống hay không, phương thức này sẽ được gọi trong các phương thức liên quan đến tải xuống file (ví dụ: downloadById, getPresignedDownloadUrl) để đảm bảo rằng chỉ những data ingestion hợp lệ mới được phép tải xuống, phương thức sẽ thực hiện các bước sau: 1) kiểm tra xem data ingestion có phải là folder hay không, nếu là folder thì không cho phép tải xuống vì folder không có file để tải, 2) kiểm tra xem data ingestion có đang ở trạng thái pending delete hay không, nếu đang ở trạng thái pending delete thì không cho phép tải xuống vì dữ liệu đang chờ xóa và có thể bị xóa bất cứ lúc nào, 3) kiểm tra xem data ingestion có minioPath hợp lệ hay không, nếu minioPath null hoặc blank thì không cho phép tải xuống vì không biết đường dẫn để tải file từ MinIO. Nếu bất kỳ điều kiện nào ở trên không th
     * @param dataIngestion
     */
    private void validateDownloadableDataIngestion(DataIngestionEntity dataIngestion) {
        if (dataIngestion.isFolder()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_FOLDER_ONLY_OPERATION);
        }
        if (DataIngestionDeleteStatus.PENDING_DELETE.equals(resolveDeleteStatus(dataIngestion))) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DELETE_IN_PROGRESS);
        }
        if (dataIngestion.getMinioPath() == null || dataIngestion.getMinioPath().isBlank()) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_DOWNLOAD_FAILED);
        }
    }

    /**
     * Định nghĩa phương thức để resolve trạng thái delete của data ingestion, phương thức này sẽ được gọi trong các phương thức liên quan đến delete để đảm bảo rằng khi kiểm tra trạng thái delete của data ingestion thì sẽ có giá trị mặc định là ACTIVE nếu trường deleteStatus trong database là null, tránh trường hợp dữ liệu bị treo ở trạng thái pending delete mãi mãi do lỗi khi lưu dữ liệu mà trường deleteStatus bị null
     * @param dataIngestion
     * @return
     */
    private DataIngestionDeleteStatus resolveDeleteStatus(DataIngestionEntity dataIngestion) {
        return dataIngestion.getDeleteStatus() == null ? DataIngestionDeleteStatus.ACTIVE : dataIngestion.getDeleteStatus();
    }

    private DataIngestionJobStatusResponseDto updateStatusAndBuildResponse(
            DataIngestionEntity dataIngestion,
            IngestionStatusResponseDto ingestionStatusResponse,
            boolean emitEvent) {
        IngestionStatus resolvedStatus = resolveStatus(ingestionStatusResponse.getStatus(), dataIngestion.getIngestionStatus());
        if (dataIngestion.getIngestionStatus() == null || !resolvedStatus.equals(dataIngestion.getIngestionStatus())) {
            dataIngestion.setIngestionStatus(resolvedStatus);
            dataIngestion = dataIngestionRepository.save(dataIngestion);
        }

        DataIngestionJobStatusResponseDto response = DataIngestionJobStatusResponseDto.builder()
                .dataIngestionId(dataIngestion.getId())
                .jobId(dataIngestion.getJobId())
                .ingestionStatus(resolvedStatus.name())
                .message(ingestionStatusResponse.getMessage())
                .build();

        // Chỉ publish event khi trạng thái ingestion có sự thay đổi và đã có owner và organization để đảm bảo rằng event được publish có đầy đủ thông tin cần thiết và tránh trường hợp publish event không cần thiết khi trạng thái ingestion không thay đổi hoặc thiếu thông tin về owner và organization
        if (emitEvent && dataIngestion.getOwner() != null && dataIngestion.getOrganization() != null) {
            systemEventSseService.publish(
                dataIngestion.getOrganization().getId(),
                dataIngestion.getOwner().getId(),
                resolveSystemEventType(resolvedStatus),
                SystemEventSource.DATA_INGESTION,
                dataIngestionMapper.entityToResponseDto(dataIngestion));
        }

        return response;
    }

    private IngestionStatus resolveStatus(String rawStatus, IngestionStatus fallbackStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return fallbackStatus == null ? IngestionStatus.CREATED : fallbackStatus;
        }

        String normalized = rawStatus.trim().toUpperCase();
        if ("SUCCESS".equals(normalized) || "DONE".equals(normalized)) {
            return IngestionStatus.COMPLETED;
        }
        if ("ERROR".equals(normalized)) {
            return IngestionStatus.FAILED;
        }

        try {
            return IngestionStatus.valueOf(normalized);
        } catch (Exception exception) {
            return fallbackStatus == null ? IngestionStatus.CREATED : fallbackStatus;
        }
    }

    private SystemEventType resolveSystemEventType(IngestionStatus status) {
        if (status == null) {
            return SystemEventType.DATA_INGESTION_STATUS_UPDATED;
        }

        if (IngestionStatus.COMPLETED.equals(status)) {
            return SystemEventType.DATA_INGESTION_COMPLETED;
        }

        if (IngestionStatus.FAILED.equals(status)) {
            return SystemEventType.DATA_INGESTION_FAILED;
        }

        return SystemEventType.DATA_INGESTION_STATUS_UPDATED;
    }

    private String resolveCallbackUrl(String requestCallbackUrl) {
        if (requestCallbackUrl != null && !requestCallbackUrl.trim().isEmpty()) {
            return requestCallbackUrl.trim();
        }

        if (appProperties.getIntegration() == null
                || appProperties.getIntegration().getDataIngestionCallback() == null
                || appProperties.getIntegration().getDataIngestionCallback().getUrl() == null
                || appProperties.getIntegration().getDataIngestionCallback().getUrl().trim().isEmpty()) {
            return null;
        }

        return appProperties.getIntegration().getDataIngestionCallback().getUrl().trim();
    }

    private DataIngestionEntity getDataIngestionEntity(UUID dataIngestionId) {
        return dataIngestionRepository.findById(dataIngestionId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.DATA_INGESTION_NOT_EXISTS));
    }

    /**
     * Định nghĩa phương thức để resolve hoặc tạo mới cây thư mục dựa trên đường dẫn tương đối truyền vào, phương thức này sẽ được gọi khi ingest một file mới với đường dẫn tương đối chứa các thư mục cha, phương thức sẽ thực hiện các bước sau: 1) nếu đường dẫn tương đối là null thì trả về null vì không có thư mục cha nào cả, 2) nếu đường dẫn tương đối không null thì sẽ tách đường dẫn thành các segment và duyệt qua từng segment để resolve hoặc tạo mới node thư mục tương ứng trong database, 3) với mỗi segment, sẽ kiểm tra xem đã tồn tại một node thư mục nào có tên giống segment đó và cùng parentId (hoặc parentId null nếu segment đó là thư mục gốc) hay chưa, nếu đã tồn tại thì sử dụng node đó làm parent cho segment tiếp theo, nếu chưa tồn tại thì tạo mới một node thư mục với tên là segment đó, parentId là node thư mục đã resolve được ở bước trước đó (hoặc null nếu segment đó là thư mục gốc), owner và organization lấy từ tham số truyền vào, accessLevel lấy từ tham số truyền vào, sau đó lưu vào database và sử dụng node mới tạo làm parent cho segment tiếp theo. Cuối cùng sau khi duyệt hết tất cả các segment thì sẽ trả về node thư mục cuối cùng đã resolve hoặc tạo mới được để làm parent cho file cần ingest
     * @param relativeParentPath
     * @param owner
     * @param organization
     * @param accessLevel
     * @return
     */
    private DataIngestionEntity resolveOrCreateFolderTree(
            Path relativeParentPath,
            UserEntity owner,
            OrganizationEntity organization,
            DataScope accessLevel,
            DataSource fromSource) {
        if (relativeParentPath == null) {
            return null;
        }

        DataIngestionEntity currentParent = null;
        for (Path segment : relativeParentPath) {
            String folderName = segment.toString().trim();
            if (folderName.isEmpty()) {
                continue;
            }
            currentParent = resolveOrCreateFolderNode(folderName, currentParent, owner, organization, accessLevel, fromSource);
        }

        return currentParent;
    }

    /**
     * Định nghĩa phương thức để resolve hoặc tạo mới một node thư mục dựa trên tên thư mục và parent, phương thức này sẽ được gọi trong phương thức resolveOrCreateFolderTree để xử lý từng segment của đường dẫn tương đối, phương thức sẽ thực hiện các bước sau: 1) kiểm tra xem đã tồn tại một node thư mục nào có tên giống folderName và cùng parentId (hoặc parentId null nếu parent là null) hay chưa, nếu đã tồn tại thì trả về node đó, nếu chưa tồn tại thì tạo mới một node thư mục với tên là folderName, parentId là id của parent (hoặc null nếu parent là null), owner và organization lấy từ tham số truyền vào, accessLevel lấy từ tham số truyền vào, sau đó lưu vào database và trả về node mới tạo. Phương thức này sẽ đảm bảo rằng không có hai node thư mục nào có cùng tên và cùng
     * @param folderName
     * @param parent
     * @param owner
     * @param organization
     * @param accessLevel
     * @return
     */
    private DataIngestionEntity resolveOrCreateFolderNode(
            String folderName,
            DataIngestionEntity parent,
            UserEntity owner,
            OrganizationEntity organization,
            DataScope accessLevel,
            DataSource fromSource) {
        Optional<DataIngestionEntity> existing = parent == null
                ? dataIngestionRepository.findFirstByFolderTrueAndNameAndParentIsNullAndOwnerIdAndOrganizationIdAndDeleteStatus(
                        folderName,
                        owner.getId(),
                        organization.getId(),
                        DataIngestionDeleteStatus.ACTIVE)
                : dataIngestionRepository.findFirstByFolderTrueAndNameAndParentIdAndOwnerIdAndOrganizationIdAndDeleteStatus(
                        folderName,
                        parent.getId(),
                        owner.getId(),
                        organization.getId(),
                        DataIngestionDeleteStatus.ACTIVE);

        if (existing.isPresent()) {
            return existing.get();
        }

        DataIngestionEntity folder = new DataIngestionEntity();
        folder.setName(folderName);
        folder.setFolder(true);
        folder.setContentType(null);
        folder.setFileSize(0L);
        folder.setMinioPath(null);
        folder.setAccessLevel(accessLevel);
        folder.setFromSource(fromSource);
        folder.setOwner(owner);
        folder.setOrganization(organization);
        folder.setJobId(null);
        folder.setIngestionStatus(null);
        folder.setDeleteStatus(DataIngestionDeleteStatus.ACTIVE);
        folder.setParent(parent);
        return dataIngestionRepository.save(folder);
    }

    /**
     * Định nghĩa phương thức để kiểm tra xem một node có phải là descendant của một node khác hay không, phương thức này sẽ được gọi trong phương thức updateFolder khi di chuyển một thư mục sang parent mới để đảm bảo rằng không tạo ra vòng lặp trong cây thư mục (ví dụ: di chuyển một thư mục cha vào trong một thư mục con của nó), phương thức sẽ thực hiện các bước sau: 1) bắt đầu từ candidateParent, duyệt lên trên theo hướng parent cho đến khi gặp null, 2) nếu trong quá trình duyệt lên mà gặp node có id giống với node đang được di chuyển thì có nghĩa là candidateParent là descendant của node và việc di chuyển sẽ tạo ra vòng lặp, do đó trả về true để báo lỗi, 3) nếu duyệt lên đến cùng mà không gặp node nào có id giống với node đang được di chuyển thì có nghĩa là candidateParent không phải là descendant của node và việc di chuyển là hợp lệ, do đó trả về false. Phương thức này sẽ giúp đảm bảo tính toàn vẹn của cây thư mục và tránh các lỗi liên quan đến vòng lặp trong cây thư mục
     * @param candidateParent
     * @param node
     * @return
     */
    private boolean isDescendantOf(DataIngestionEntity candidateParent, DataIngestionEntity node) {
        DataIngestionEntity cursor = candidateParent;
        while (cursor != null) {
            if (cursor.getId().equals(node.getId())) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }
}
