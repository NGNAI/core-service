# Alertmanager — Routing & Notification

**Alertmanager** nhận **alert** từ Prometheus, gom nhóm (grouping), lọc (inhibit), và gửi **notification** (email, Slack, ...) tới người nhận.

- **UI:** http://localhost:9093
- **Port:** `9093`

---

## Nó dùng để làm gì?

- Nhận alert từ Prometheus (khi alert rule firing).
- **Grouping:** gom nhiều alert cùng loại thành 1 notification (tránh spam).
- **Routing:** định tuyến alert tới receiver phù hợp theo label (vd severity).
- **Inhibit:** tắt các alert phụ khi có alert chính (vd app down → tắt alert 5xx).
- **Silencing:** tạm thời im lặng alert (qua UI).

---

## Cấu hình chính — `alertmanager.yml`

### 1. Global
```yaml
global:
  resolve_timeout: 5m   # thời gian chờ xác nhận alert đã resolved
```

### 2. Route — định tuyến alert
```yaml
route:
  receiver: "default"                 # receiver mặc định
  group_by: ["alertname", "severity"] # gom nhóm theo tên alert + severity
  group_wait: 30s                     # chờ 30s trước khi gửi nhóm đầu tiên
  group_interval: 5m                  # gửi lại nhóm mỗi 5m nếu còn alert
  repeat_interval: 4h                  # lặp lại notification mỗi 4h
  routes:
    - match:
        severity: critical            # alert critical → receiver "critical"
      receiver: "critical"
```

### 3. Receivers — nơi gửi notification
```yaml
receivers:
  - name: "default"
    # TODO: thêm email_configs khi cần
  - name: "critical"
    # TODO: thêm slack_configs khi cần
```

> **Hiện tại:** cả 2 receiver đều là **null receiver** (không gửi đi đâu). Alertmanager chỉ giữ state + hiển thị trên UI `:9093`. Cần gửi email/Slack thì thêm cấu hình vào (xem comment TODO trong file).

### 4. Inhibit rules — chống spam
```yaml
inhibit_rules:
  - source_match:
      alertname: CoreServiceDown      # khi app DOWN
    target_match_re:
      alertname: CoreService.*         # tắt các alert CoreService khác
    equal: ["service"]
```

> Khi `CoreServiceDown` firing → các alert `CoreServiceHighErrorRate`, `CoreServiceHighLatencyP95`, ... bị tắt để không spam.

---

## Cách xem / kiểm tra

1. **UI:** http://localhost:9093
2. **Alerts:** xem các alert đang active (firing) và trạng thái.
3. **Silences:** tạo silence tạm thời cho alert.
4. **Status:** xem cấu hình đang nạp, receivers, notification đã gửi.

---

## Cách thêm notification (email ví dụ)

Thêm vào receiver `default` trong `alertmanager.yml`:
```yaml
receivers:
  - name: "default"
    email_configs:
      - to: "ops@example.com"
        from: "alertmanager@example.com"
        smarthost: "smtp.example.com:587"
        auth_username: "alertmanager@example.com"
        auth_password: "${SMTP_PASSWORD}"
```

Sau đó restart Alertmanager:
```bash
docker compose -f monitoring/docker-compose.yml restart alertmanager
```

---

## Lưu ý

- **Null receiver hiện tại** — chỉ giữ state + UI, chưa gửi notification thật.
- **Secrets:** dùng biến môi trường (`${SMTP_PASSWORD}`, `${SLACK_WEBHOOK_URL}`) thay vì hardcode.
- **Dữ liệu** lưu trên volume `alertmanager-data` (state, silences).
