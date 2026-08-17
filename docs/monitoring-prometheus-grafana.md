# Monitoring: Prometheus + Grafana cho core-service

Hướng dẫn bật giám sát (metrics) cho `core-service` bằng **Prometheus** (thu thập metrics) và **Grafana** (dashboard trực quan).

## 1. Tổng quan

```
core-service (/api/v1/actuator/prometheus)  --scrape(basic auth)-->  Prometheus (:9090)  --query-->  Grafana (:3000)
```

- **Micrometer + `micrometer-registry-prometheus`**: expose metrics chuẩn (JVM, HTTP, process, v.v.) tại `/api/v1/actuator/prometheus`.
- **Spring Security**: endpoint prometheus nằm trong chain `/actuator/**`, yêu cầu **basic auth** bằng user `monitor`.
  - User được định nghĩa **tường minh** trong `ApplicationConfig#actuatorUserDetailsService` (`InMemoryUserDetailsManager`),
    username `monitor`, password lấy từ env `ACTUATOR_USER_PASSWORD` (mặc định `changeme`).
  - KHÔNG dùng `spring.security.user` trong yml vì project có bean `PasswordEncoder` tùy chỉnh
    khiến auto-config có thể không tạo user → basic auth luôn trả 401.
- **Spring Boot Admin** vẫn hoạt động bình thường vì `/actuator/health` và `/actuator/info` vẫn `permitAll`.

## 2. Thay đổi trong code

### `core/pom.xml`
Thêm dependency (version do Spring Boot parent quản lý):

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### `core/src/main/java/ai/configuration/ApplicationConfig.java`
User actuator được định nghĩa **tường minh** (không phụ thuộc auto-config `spring.security.user`):

```java
@Bean
UserDetailsService actuatorUserDetailsService(
        PasswordEncoder passwordEncoder,
        @Value("${ACTUATOR_USER_PASSWORD:changeme}") String actuatorPassword) {
    return new InMemoryUserDetailsManager(
            User.withUsername("monitor")
                    .password(passwordEncoder.encode(actuatorPassword))
                    .roles("ACTUATOR")
                    .build());
}
```

### `core/src/main/resources/application.yml`
- `management.endpoints.web.exposure.include` đã có sẵn `prometheus` (không cần sửa).
- Không cần `spring.security.user` — user do bean ở trên quản lý.

> **Lưu ý bảo mật:** user này chỉ dùng cho basic auth của Actuator, KHÔNG liên quan login người dùng (JWT). Trước khi lên production, đổi password qua biến môi trường `ACTUATOR_USER_PASSWORD` và không commit giá trị thật vào git.

## 3. Chạy Prometheus + Grafana

Từ thư mục gốc repo:

```bash
docker compose -f monitoring/docker-compose.yml up -d
```

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin / admin — đổi ngay sau lần đăng nhập đầu)

**Lưu ý basic auth scrape:** password `changeme` được **hardcode** trong `monitoring/prometheus/prometheus.yml`
(không dùng `${ACTUATOR_USER_PASSWORD}` vì Prometheus không expand biến môi trường vào `basic_auth.password`
đáng tin cậy — đã kiểm chứng: env trong container = `changeme` nhưng scrape vẫn trả 401).
Nếu app chạy cổng khác (vd default 8080 thay vì profile `win` = 8081), đổi `targets` cho khớp.

Kiểm tra scrape:
- Mở Prometheus → **Status → Targets** → job `core-service` phải là **UP**.
- Mở **Graph** và chạy thử query: `process_uptime_seconds`.

## 4. Grafana Dashboard

Dashboard **"Spring Boot — core-service"** được tự nạp khi Grafana khởi động nhờ provisioning:
- `monitoring/grafana/provisioning/datasources/datasource.yml` — datasource Prometheus
- `monitoring/grafana/provisioning/dashboards/spring-boot-core.json` — dashboard mẫu (JVM memory, CPU, HTTP rate/latency/error, threads, GC)

Dashboard mẫu sẽ nằm trong folder **Spring Boot**. Muốn thêm dashboard khác (đẹp/đầy đủ hơn), import dashboard ID phổ biến trong menu **Dashboards → Import**:
- **12900** — Spring Boot 3.x Statistics
- **19004** — Spring Boot Actuator
- **4701** — JVM (Micrometer)

## 5. Cấu trúc thư mục monitoring

```
monitoring/
├── docker-compose.yml                        # Prometheus + Grafana
├── prometheus/
│   └── prometheus.yml                        # scrape config (basic auth hardcode, target host.docker.internal:8081)
└── grafana/
    └── provisioning/
        ├── datasources/datasource.yml        # datasource Prometheus (uid: prometheus)
        └── dashboards/
            ├── dashboard.yml                 # provider nạp dashboard tự động
            └── spring-boot-core.json         # dashboard mẫu
```

## 6. Gỡ cài đặt

```bash
docker compose -f monitoring/docker-compose.yml down -v   # -v xóa luôn volume data
```

## 7. Business metrics tùy chỉnh (@Timed / @Counted)

Ngoài metrics HTTP mặc định (`http.server.requests`), ta đo riêng các nghiệp vụ quan trọng bằng annotation Micrometer.

### Kích hoạt aspect — `core/src/main/java/ai/configuration/MetricsConfig.java`

```java
@Configuration
public class MetricsConfig {
    @Bean public TimedAspect timedAspect(MeterRegistry registry) { return new TimedAspect(registry); }
    @Bean public CountedAspect countedAspect(MeterRegistry registry) { return new CountedAspect(registry); }
}
```

### Các method đang được đo

| Method | @Counted | @Timed | Metric Prometheus |
|---|---|---|---|
| `AuthService.auth` (login) | `auth.login.attempts` | `auth.login` | `auth_login_attempts_total`, `auth_login_seconds_*` |
| `TopicService.getAll` | `api.topic.list.calls` | `api.topic.list` | `api_topic_list_calls_total`, `api_topic_list_seconds_*` |
| `NoteBookService.getAll` | `api.notebook.list.calls` | `api.notebook.list` | `api_notebook_list_calls_total`, `api_notebook_list_seconds_*` |
| `DraftService.getAll` | `api.draft.list.calls` | `api.draft.list` | `api_draft_list_calls_total`, `api_draft_list_seconds_*` |

- Tag tự động của @Counted/@Timed: `class`, `method`, `exception`, `result` (`success`/`failure`).
- Counter đếm thất bại qua `exception` (vd login sai → `exception="AppException"`, `result="failure"`).
- Timer có histogram (`_bucket`) vì đã bật `management.metrics.distribution.percentiles-histogram` cho các tên này trong `application.yml` → Grafana tính được p95.

### Cách thêm metric cho API/service khác

```java
@Counted(value = "api.orders.fetch.calls", description = "Số lần get list đơn hàng")
@Timed(value = "api.orders.fetch", description = "Thời gian get list đơn hàng")
public List<OrderDto> getAll(OrderFilterDto filterDto) { ... }
```

Rồi thêm panel vào `spring-boot-core.json` với query:
```promql
sum(rate(api_orders_fetch_calls_total[$__rate_interval]))            # rate
histogram_quantile(0.95, sum(rate(api_orders_fetch_seconds_bucket[$__rate_interval])) by (le))  # p95
```

> Ghi chú: metric chỉ xuất hiện sau khi method được gọi lần đầu (Micrometer lazy-register), và Prometheus tự nhận metric mới khi scrape lại (mỗi 15s) — không cần sửa config Prometheus.
