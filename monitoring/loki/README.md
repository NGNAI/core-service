# Loki — Lưu trữ & Truy vấn Logs

**Loki** là hệ thống quản lý **logs** (nhật ký) của Grafana. Khác với Prometheus (lưu metrics), Loki lưu **log text** và cho phép truy vấn bằng **LogQL**. Nó được thiết kế nhẹ, gắn liền với Grafana.

- **Port:** `3100`
- **UI:** không có UI riêng — truy vấn qua **Grafana Explore** (datasource Loki).

---

## Nó dùng để làm gì?

- Nhận log từ `core-service` (qua Logback appender HTTP push tới Loki).
- Lưu trữ log dạng chunk (nén snappy) trên filesystem.
- Cho phép truy vấn log theo label (app, level, ...) và tìm kiếm nội dung qua Grafana.

---

## Cấu hình chính — `loki-config.yml`

Đây là cấu hình **dev, không auth** (`auth_enabled: false`).

| Phần | Giá trị | Ý nghĩa |
|------|---------|---------|
| `server.http_listen_port` | `3100` | Cổng nhận log & query |
| `ingester` | `wal.enabled: false`, `chunk_idle_period: 5m` | Cách gom log thành chunk |
| `schema_config` | `store: boltdb-shipper`, `object_store: filesystem`, `schema: v11` | Lưu index + chunk trên filesystem |
| `storage_config.filesystem.directory` | `/loki/chunks` | Thư mục lưu chunk |
| `common.path_prefix` | `/loki` | Đường dẫn gốc dữ liệu |
| `limits_config` | `reject_old_samples: true`, `reject_old_samples_max_age: 168h` | Từ chối log quá cũ (> 7 ngày) |

> **Lưu ý:** `allow_structured_metadata: false` — không hỗ trợ metadata có cấu trúc (giữ cấu hình đơn giản).

---

## Cách hoạt động

1. **Push log:** `core-service` gửi log tới Loki qua HTTP (Logback appender). Log được gắn **labels** (vd `app="NGN AI core"`, `level="ERROR"`).
2. **Lưu trữ:** Loki gom log thành chunk, nén và lưu trên filesystem (`/loki/chunks`).
3. **Query:** Grafana gửi query LogQL tới Loki → Loki tìm log theo label + nội dung.

---

## Cách xem / truy vấn log

1. Mở **Grafana** → http://localhost:3000
2. Vào menu **Explore**
3. Chọn datasource **Loki**
4. Nhập query LogQL, ví dụ:
   - `{app="NGN AI core"}` — tất cả log của app
   - `{app="NGN AI core", level="ERROR"}` — chỉ log ERROR
   - `{app="NGN AI core"} |= "Exception"` — log chứa từ "Exception"
   - `count_over_time({app="NGN AI core", level="ERROR"}[5m])` — đếm log ERROR trong 5 phút

---

## Lưu ý

- **Không có auth** — chỉ dùng cho dev. Production cần bật auth (multi-tenancy) hoặc đặt sau reverse proxy.
- **Dữ liệu lưu trên volume** `loki-data` (docker volume) — xóa volume sẽ mất log.
- **Liên kết log ↔ metric:** Grafana datasource Loki có `derivedFields` trích `request_id` → click vào request_id trong log sẽ mở query Prometheus tương ứng.
