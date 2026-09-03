# Grafana — Dashboard & Alerting

**Grafana** là nền tảng trực quan hóa dữ liệu. Nó query **Prometheus** (metrics) và **Loki** (logs) để vẽ dashboard, đồng thời là nơi tạo **alert rules** và quản lý cảnh báo.

- **UI:** http://localhost:3000
- **Login:** `admin` / `admin`
- **Port:** `3000`

---

## Nó dùng để làm gì?

- **Dashboard:** hiển thị metrics của `core-service` (request, latency, JVM, ...) dạng biểu đồ.
- **Explore:** query trực tiếp PromQL (metrics) hoặc LogQL (logs) để debug.
- **Alerting:** tạo alert rules, quản lý notification policies, xem lịch sử cảnh báo.

---

## Provisioning — tự nạp khi khởi động

Grafana được cấu hình tự nạp cấu hình từ volume `./grafana/provisioning` (qua `GF_PATHS_PROVISIONING`). Có 3 phần:

### 1. Datasources — `provisioning/datasources/datasource.yml`

Tự đăng ký 2 nguồn dữ liệu:

| Datasource | UID | URL | Vai trò |
|-----------|-----|-----|---------|
| **Prometheus** | `prometheus` | `http://prometheus:9090` | Metrics (mặc định) |
| **Loki** | `loki` | `http://loki:3100` | Logs |

> Loki có `derivedFields` để trích `request_id` từ log → click vào request_id trong log sẽ mở query Prometheus tương ứng (trace liên kết log ↔ metric).

### 2. Dashboards — `provisioning/dashboards/dashboard.yml`

- Tự nạp mọi file JSON trong thư mục `dashboards/` vào folder **"Spring Boot"**.
- File hiện có: `spring-boot-core.json` (dashboard Spring Boot cho core-service).
- `updateIntervalSeconds: 30` → tự cập nhật khi sửa file JSON.
- `allowUiUpdates: true` → có thể chỉnh sửa trên UI.

### 3. Alerting — `provisioning/alerting/alerting.yml`

> **QUAN TRỌNG:** File này **cố tình chỉ chứa comments** (không có rule thật).

Lý do: trên **Grafana 13** (kiến trúc alerting mới / unistore), provisioning alert rules qua file **KHÔNG tạo được rules hiệu quả** (log báo success nhưng API vẫn trả `Groups: 0`).

→ **Alert rules nên tạo qua Grafana UI:** `Alerting → Alert rules → New alert rule`.

---

## Cách tạo Alert qua UI (khuyến nghị)

Vào **Alerting → Alert rules → New alert rule**, chọn datasource **Prometheus** (hoặc **Loki**), nhập query:

| Alert | Query (PromQL) | Ngưỡng |
|-------|---------------|--------|
| App down | `up{job="core-service"}` | `< 1` (for 2m, critical) |
| HTTP 5xx cao | `rate(http_server_requests_seconds_count{status=~"5.."}[5m])` | `> 0.1` |
| Latency p95 | `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))` | `> 2` |
| JVM heap cao | `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}` | `> 0.85` |
| Log ERROR tăng (Loki) | `count_over_time({app="NGN AI core", level="ERROR"}[5m])` | `> 5` |

---

## Cách xem / sử dụng

1. **Dashboard:** vào **Dashboards → Spring Boot** → chọn dashboard core-service.
2. **Explore:** menu **Explore** → chọn datasource Prometheus/Loki → nhập query.
3. **Alerting:** menu **Alerting** → xem rules, instances (đang firing), notification policies.

---

## Lưu ý

- **Login mặc định** `admin/admin` — nên đổi trong production (qua `GF_SECURITY_ADMIN_PASSWORD`).
- **Sign-up bị tắt** (`GF_USERS_ALLOW_SIGN_UP: "false"`).
- Dashboard JSON có thể chỉnh trên UI rồi export lại để cập nhật file provisioning.
