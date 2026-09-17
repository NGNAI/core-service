# Ingestion status sync — cơ chế dự phòng (fallback)

## Vấn đề

Scheduler `DataIngestionMaintenanceScheduler.syncIngestionStatuses` và
`NoteBookIngestionMaintenanceScheduler.syncVectorStatuses` chạy **mỗi phút**, quét các
record đang ở trạng thái ingestion **non-final** (CREATED/EXTRACTING/CHUNKING/EMBEDDING/STORING)
rồi gọi `IngestionService.getJobStatus(jobId)` để đồng bộ trạng thái mới nhất từ RAG service.

Trước đây `getJobStatus` gộp **mọi** lỗi vào một exception duy nhất:

```java
catch (RestClientException exception) {
    exception.printStackTrace();
    throw new AppException(ApiResponseStatus.INGESTION_SERVICE_UNAVAILABLE);
}
```

Hệ quả: khi RAG trả **404 `Job 'xxx' not found`** (job đã bị xóa, service restart mất state,
hoặc jobId cũ) thì:

1. Exception bị bắt trong scheduler, chỉ `log.error(...)` rồi bỏ qua;
2. **DB không được cập nhật** → record vẫn ở trạng thái non-final;
3. Phút sau scheduler quét lại đúng record đó → lại 404 → lại log lỗi…

→ **Vòng lặp vô hạn**, log lỗi lặp lại không có hồi kết, đồng thời `printStackTrace()` mỗi
phút làm ngập log.

## Giải pháp

### 1. Phân loại lỗi ở tầng `IngestionService`

`getJobStatus` phân biệt rõ 2 loại lỗi:

| Loại | HTTP | Ném exception | Ý nghĩa |
|---|---|---|---|
| **Vĩnh viễn** | `404` | `INGESTION_JOB_NOT_FOUND` + `isNotFound() == true` | Poll lại **chắc chắn** vẫn thất bại → dừng ngay |
| **Tạm thời** | timeout / không kết nối / 5xx | `INGESTION_SERVICE_UNAVAILABLE` | Có thể thành công ở lần sau → retry có đếm |

`IngestionServiceException` mang thêm `httpStatusCode` để bên gọi kiểm tra qua
`isNotFound()`. Không dùng `printStackTrace()` nữa — chỉ log **một dòng** `warn` có ngữ cảnh.

### 2. Cơ chế dự phòng trong scheduler

Cả hai luồng (`DataIngestionService.syncSingleIngestionStatus`,
`NoteBookSourceService.syncPendingVectorStatuses`) áp dụng **3 lớp chốt**:

| Lớp | Điều kiện | Hành động |
|---|---|---|
| **404 job not found** | RAG không còn job | Đánh dấu `FAILED` **ngay** → dừng vòng lặp |
| **Lỗi tạm thời liên tiếp** | `status_sync_failure_count >= max-status-sync-failures` (mặc định **5**) | Đánh dấu `FAILED` |
| **Kẹt trạng thái non-final** | Record không đổi trạng thái trong `max-status-stale-minutes` (mặc định **180 phút**) | Đánh dấu `FAILED` |

Bộ đếm `status_sync_failure_count` **reset về 0** ngay khi đọc trạng thái thành công (kể cả
trạng thái vẫn non-final), nên chỉ tính lỗi **liên tiếp**.

Lớp 3 dùng `audit.updatedAt` (chỉ được làm mới khi trạng thái **thực sự thay đổi**) làm mốc
tiến độ — chặn trường hợp RAG trả về mãi một trạng thái trung gian mà không bao giờ final.

### 3. Vòng lặp source-guide (NotebookLM)

`syncCompletedSourceGuides` trước đây có 2 lỗ hổng gây poll vô hạn:

- RAG trả `status=failed` → rơi vào nhánh `else` chỉ `log.debug` → **không tăng retry** →
  source kẹt ở `PROCESSING` → GET lại mãi mỗi phút.
- RAG trả `status=processing` mãi → cũng không tăng retry.

Đã sửa:

- Nhánh `failed` → gọi `registerSummaryFailure(...)` để bộ đếm tiến dần tới `maxRetry`.
- Thêm cột `summary_processing_started_at` (ghi một lần khi bắt đầu chờ) + chốt
  `isSummaryProcessingStale(...)` → đánh dấu `FAILED` khi chờ quá `max-status-stale-minutes`.

### 4. API retry thủ công

Khi record bị đánh dấu `FAILED` do RAG mất job, cần đường khôi phục:

| Endpoint | Mô tả |
|---|---|
| `POST /user/data-ingestion/{id}/ingestion/retry` | Đã có sẵn (chỉ cho phép khi `FAILED`) |
| `POST /user/notebooks/{noteBookId}/sources/{sourceId}/ingestion/retry` | **Mới** — reset `jobId`, các bộ đếm, rồi dispatch lại source lên RAG |

## Cấu hình

```yaml
maintenance:
  max-delete-retries: 5
  max-dispatch-retries: 5
  # Số lần đồng bộ trạng thái thất bại LIÊN TIẾP tối đa trước khi đánh dấu FAILED
  max-status-sync-failures: 5
  # Số phút tối đa một job được phép kẹt ở trạng thái trung gian
  max-status-stale-minutes: 180
```

Đặt `0` hoặc giá trị âm để dùng mặc định.

## Schema

Migration `V35__add_ingestion_status_sync_tracking.sql`:

- `data_ingestion.status_sync_failure_count` (int4, default 0)
- `notebook_sources.status_sync_failure_count` (int4, default 0)
- `notebook_sources.summary_processing_started_at` (timestamptz)

## Error codes mới

| Code | Enum | HTTP |
|---|---|---|
| 1201 | `INGESTION_JOB_NOT_FOUND` | 404 |
| 1202 | `INGESTION_STATUS_STALE` | 409 |
| 1203 | `INGESTION_STATUS_SYNC_EXHAUSTED` | 409 |

## Hệ quả vận hành

Sau khi triển khai, các record đang kẹt sẽ tự chuyển sang `FAILED` trong tối đa
`max-status-sync-failures` phút (với lỗi tạm thời) hoặc **ngay lập tức** (với 404).
Người dùng thấy trạng thái `FAILED` kèm `ingestion_error` mô tả nguyên nhân, và có thể bấm
retry để khôi phục — thay vì log lỗi lặp lại vô hạn như trước.
