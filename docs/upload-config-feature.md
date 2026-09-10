# Upload Config — Rào chắn upload file

Tính năng thêm rào chắn upload file để bảo vệ máy chủ khi giờ cao điểm: giới hạn **số lượng file mỗi lần upload**, **dung lượng mỗi file**, **loại file được phép**, và (Notebook) **tổng số source**.

## Cấu hình theo từng loại

Cấu hình lưu trong **System Settings DB** (group `UPLOAD`, `isPublic=true` — admin chỉnh qua `/admin/system-settings`, FE đọc qua `/public/settings`). Mặc định fallback trong code (`UploadConfigService`).

| Key | Mặc định | Ý nghĩa |
|---|---|---|
| `upload.topic.maxFileCount` | 3 | Số file tối đa/lần upload chat Topic |
| `upload.topic.maxFileSizeMb` | 100 | Dung lượng tối đa mỗi file (MB) |
| `upload.topic.allowedFileTypes` | "" | Extension cho phép (rỗng = tất cả) |
| `upload.notebook.maxFileCount` | 10 | Số file tối đa/lần upload chat Notebook |
| `upload.notebook.maxFileSizeMb` | 100 | Dung lượng tối đa mỗi file (MB) |
| `upload.notebook.maxTotalSources` | 100 | Tổng số source tối đa trong mỗi Notebook |
| `upload.notebook.allowedFileTypes` | "" | Extension cho phép |
| `upload.draft.maxFileCount` | 3 | Số file tối đa/lần upload Draft |
| `upload.draft.maxFileSizeMb` | 100 | Dung lượng tối đa mỗi file (MB) |
| `upload.draft.allowedFileTypes` | "" | Extension cho phép |
| `upload.dataIngestion.maxFileCount` | 10 | Số file tối đa/lần upload Data-Ingestion |
| `upload.dataIngestion.maxFileSizeMb` | 100 | Dung lượng tối đa mỗi file (MB) |
| `upload.dataIngestion.allowedFileTypes` | "" | Extension cho phép |

> **Quy ước:** value = `0` → **unlimited** (bỏ qua check tương ứng).

## Nơi áp dụng rào chắn

| Luồng | Service method | Rào chắn |
|---|---|---|
| Topic (chat + sources) | `TopicSourceService.uploadSources` | count + per-file size/type |
| Notebook (sources) | `NoteBookSourceService.addFileSources` | count + per-file size/type + tổng source |
| Notebook (text source) | `NoteBookSourceService.addTextSource` | tổng source |
| Notebook (note source) | `NoteBookSourceService.addNoteSources` | tổng source |
| Draft (chat + sources) | `DraftSourceService.uploadSources` | count + per-file size/type |
| Data-Ingestion | `DataIngestionService.uploadDataIngestion` | count + per-file size/type |

Chat inline (Topic/Draft) dùng chung `uploadSources` nên tự động được rào. Giới hạn tổng source notebook (`maxTotalSources`) áp dụng cho **cả 3 luồng tạo source**: upload file, thêm text, thêm từ note.

## Error codes

- `FILE_COUNT_EXCEEDED` (1197) — vượt số file/lần
- `TOTAL_SOURCES_EXCEEDED` (1198) — vượt tổng source notebook
- `INVALID_UPLOAD_TYPE` (1199) — type không hợp lệ
- Tái dùng: `FILE_SIZE_EXCEEDED` (1164), `FILE_TYPE_NOT_ALLOWED` (1165)

## API cho frontend

### Authenticated (JWT) — `GET /user/upload-config`
- `GET /user/upload-config/{type}` — config 1 loại (`TOPIC`/`NOTEBOOK`/`DRAFT`/`DATA_INGESTION`)
- `GET /user/upload-config` — config tất cả loại

Response `UploadConfigResponseDto`: `{ type, maxFileCount, maxFileSizeMb, maxTotalSources, allowedFileTypes }`.

### Public — `GET /public/settings/map`
Các key `upload.*` có `isPublic=true` nên FE đọc được qua endpoint public có sẵn (không cần xác thực).

## Lưu ý

- **Giới hạn cứng HTTP:** `spring.servlet.multipart.max-file-size` / `max-request-size` = `100MB` (application.yml) chặn file >100MB trước khi vào controller. Nếu muốn unlimited thật sự >100MB phải nâng cấp cấu hình multipart.
- **Data-Ingestion upload nhiều file:** DTO `DataIngestionUploadRequestDto` dùng `MultipartFile[] files`, endpoint trả `List<DataIngestionResponseDto>`.
- **Auto-import** (`DataIngestionAutoImportScheduler`) **không** bị rào (chỉ rào API upload qua HTTP).
- Migration seed: `V31__add_upload_config_settings.sql`.
