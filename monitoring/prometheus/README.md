# Prometheus — Thu thập Metrics

**Prometheus** là hệ thống giám sát **metrics** (số liệu đo lường). Nó **kéo (scrape)** các endpoint metrics theo chu kỳ, lưu trữ trong time-series database, và đánh giá **alert rules** để phát hiện sự cố.

- **UI:** http://localhost:9090
- **Port:** `9090`

---

## Nó dùng để làm gì?

- Thu thập metrics từ `core-service` (qua Micrometer → `/api/v1/actuator/prometheus`): HTTP request rate, latency, JVM heap, thread, DB pool, v.v.
- Lưu trữ metrics dạng time-series (có thể query bằng PromQL).
- Đánh giá **alert rules** và gửi alert sang **Alertmanager** khi vượt ngưỡng.

---

## Cấu hình chính — `prometheus.yml`

### 1. Global
```yaml
global:
  scrape_interval: 15s      # scrape mỗi 15s
  evaluation_interval: 15s  # đánh giá alert rule mỗi 15s
```

### 2. Alerting — gửi alert tới Alertmanager
```yaml
alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - "alertmanager:9093"   # tên service trong docker-compose
```

### 3. Rule files
```yaml
rule_files:
  - /etc/prometheus/rules/*.yml   # mount từ ./prometheus/rules
```

### 4. Scrape config — job `core-service`
```yaml
- job_name: "core-service"
  metrics_path: "/api/v1/actuator/prometheus"  # context-path /api/v1 + /actuator
  static_configs:
    - targets:
        - "host.docker.internal:${PROMETHEUS_APP_PORT:-8080}"  # app chạy trên host
      labels:
        application: "NGN AI core"
        module: "core"
  basic_auth:
    username: "monitor"
    password: "changeme"   # phải khớp ACTUATOR_USER_PASSWORD của app
```

> **Giải thích `host.docker.internal`:** app chạy trên host (không phải container), nên Prometheus trong container dùng `host.docker.internal` để trỏ về máy host. Trên Windows/Mac cần `extra_hosts: host.docker.internal:host-gateway` (đã có trong docker-compose).

---

## Alert rules — `rules/core-service.yml`

Prometheus tự đánh giá các rule này và gửi alert sang Alertmanager:

| Alert | Điều kiện | Severity | Ý nghĩa |
|-------|----------|----------|---------|
| `CoreServiceDown` | `up{job="core-service"} == 0` (2m) | critical | App không scrape được metrics |
| `CoreServiceHighErrorRate` | HTTP 5xx > 5% (5m) | warning | Tỷ lệ lỗi 5xx cao |
| `CoreServiceHighLatencyP95` | p95 latency > 2s (5m) | warning | Request chậm |
| `CoreServiceHighHeapUsage` | JVM heap > 85% (5m) | warning | Nguy cơ OOM |
| `CoreServiceHealthDown` | App vẫn down sau 5m | critical | Cần can thiệp thủ công |

---

## Cách xem / kiểm tra

1. **Mở UI:** http://localhost:9090
2. **Kiểm tra target:** menu **Status → Targets** — xem job `core-service` có `UP` không.
3. **Query PromQL:** tab **Graph**, ví dụ:
   - `up` — trạng thái các target
   - `rate(http_server_requests_seconds_count[5m])` — request rate
   - `jvm_memory_used_bytes{area="heap"}` — heap usage
4. **Xem alert:** menu **Alerts** — xem trạng thái các rule (inactive/pending/firing).

---

## Lưu ý

- **Basic auth:** Prometheus đọc file env lúc start, KHÔNG expand biến env vào `password` field đáng tin cậy → dùng giá trị mặc định `changeme` cho dev. **Production nên dùng `password_file`** (bind mount, chmod 600).
- **Port:** mặc định `8080`; khi chạy profile `win` dùng `8081` (đổi qua `PROMETHEUS_APP_PORT`).
- **Alert chủ yếu tạo qua Grafana UI** (xem [Grafana README](../grafana/README.md)) — file rules này là tùy chọn bổ sung.
