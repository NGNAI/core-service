# Proxy API service ngoài qua core-service (Spring Cloud Gateway MVC)

Tài liệu mô tả cách `core-service` làm **đầu mối duy nhất** cho frontend: proxy các API của
service nội bộ (Spellcheck/RAG service) và gộp tài liệu OpenAPI về chung một Swagger UI.

## 1. Vì sao dùng Gateway bản MVC

App chạy Tomcat + Spring MVC (không phải WebFlux), nên phải dùng Gateway **servlet**:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-server-webmvc</artifactId>
</dependency>
```

> Từ Spring Cloud **2025.1.x** (dòng cho Spring Boot 4), artifact **đổi tên**:
> `spring-cloud-starter-gateway-mvc` → `spring-cloud-starter-gateway-server-webmvc`.
> Dùng tên cũ sẽ không resolve được (thiếu version).

Version BOM ở `pom.xml` gốc:

```xml
<spring-cloud.version>2025.1.4</spring-cloud.version>
```

Bảng tương thích chính thức (https://spring.io/projects/spring-cloud):

| Spring Cloud | Spring Boot |
|---|---|
| 2025.1.x (Oakwood) | **4.0.x, 4.1.x** (từ 2025.1.2) |
| 2025.0.x (Northfields) | 3.5.x |
| 2024.0.x | 3.4.x |
| 2023.0.x | 3.2.x, 3.3.x |

Nếu chọn sai (vd 2025.0.x cho Boot 4) sẽ lỗi:

```
IllegalStateException: Unable to read meta-data for class
  org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
```

vì Spring Boot 4 đã module hóa, class chuyển sang
`org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration`.

## 2. Cấu hình route

```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:                    # prefix ĐÚNG cho Gateway MVC 5.x
          http-client:
            read-timeout: "10m"    # đủ dài cho SSE /api/jobs/{id}/events
            connect-timeout: "10s"
          routes:
            - id: rag-spellcheck-proxy
              uri: "http://192.168.30.73:8000"
              predicates:
                - Path=/spellcheck/**
              filters:
                - RewritePath=/api/v1/spellcheck/(?<seg>.*), /${seg}
```

### Các bẫy đã gặp (ghi lại để không lặp lại)

| Sai | Triệu chứng | Đúng |
|---|---|---|
| `spring.cloud.gateway.routes` | **Im lặng** không có tác dụng (đó là key của WebFlux) | `spring.cloud.gateway.server.webmvc.routes` |
| `spring.cloud.gateway.mvc.routes` | Deprecated từ Spring Cloud 4.2+ | `spring.cloud.gateway.server.webmvc.routes` |
| `StripPrefix=1` khi app có `server.servlet.context-path` | Cắt **sai** segment → upstream trả 404, nhưng response có header `Server: uvicorn` (chứng tỏ proxy đã forward) | `RewritePath` với regex gồm cả context-path |
| Trả `JsonNode` từ controller | Jackson serialize thành `{"array":false,"boolean":false,...}` | Trả `String` JSON |
| `produces = APPLICATION_JSON_VALUE` | Tiếng Việt bị mojibake | `APPLICATION_JSON_VALUE + ";charset=UTF-8"` |
| Inject `WebClient.Builder` | `APPLICATION FAILED TO START: required a bean of type 'WebClient$Builder'` | Dùng `WebClient.builder()` |
| `spring.cloud.gateway.server.webmvc.enabled=false` | `APPLICATION FAILED TO START: Parameter 0 of constructor in ...FilterAutoConfiguration$RetryFilterConfiguration required a bean of type 'GatewayMvcProperties'` | Exclude đủ 7 auto-configuration (mục 8) |

**Vì sao `StripPrefix` cắt sai:** `BeforeFilterFunctions.lambda$stripPrefix` thao tác trên
`request.uri().getRawPath()` — URI này **bao gồm** context-path (`/api/v1/spellcheck/...`).
Trong khi đó `Path` predicate lại nhìn path **đã bỏ** context-path (`/spellcheck/...`).
→ Hai bên nhìn path khác nhau, nên `StripPrefix` cắt mất `/api` chứ không phải `spellcheck`.

## 3. Bảo mật

Service đích (spellcheck) **không có xác thực**. Vì vậy route proxy bắt buộc đi qua
filter chain JWT của core-service:

```java
// SecurityConfig#accessFilterChain
.securityMatcher("/admin/**", "/user/**", "/category/**", "/spellcheck/**")
```

Luồng:

```
FE --(JWT)--> core-service /api/v1/spellcheck/api/health
                 |
                 +-- accessFilterChain: verify JWT -> 401 nếu thiếu/sai
                 |
                 +-- Gateway route: RewritePath -> /api/health
                         |
                         +--> http://192.168.30.73:8000/api/health
```

## 4. Gộp tài liệu OpenAPI vào chung Swagger UI

### Vấn đề
`springdoc.swagger-ui.urls` nếu trỏ thẳng `http://192.168.30.73:8000/openapi.json` thì
**trình duyệt** phải gọi vào IP nội bộ → thường không reachable, và bị CORS chặn.

### Giải pháp
Thêm endpoint public trả spec hộ, có **rewrite `servers`** để nút "Try it out" gọi qua
route proxy:

```
GET /api/v1/public/docs/spellcheck     (public, không cần JWT)
```

Cấu hình Swagger UI:

```yaml
springdoc:
  swagger-ui:
    urls:
      - name: Main System APIs
        url: /v3/api-docs                        # tương đối -> browser tự ghép origin
      - name: Spellcheck Service (via core-service)
        url: /public/docs/spellcheck             # core-service gọi hộ upstream
```

Cấu hình service:

```yaml
spellcheck:
  url: "${SPELLCHECK_URL:http://192.168.30.73:8000}"   # PHẢI trùng uri của route gateway
  openapi-path: "/openapi.json"                        # FastAPI mặc định
```

Kết quả spec trả về có `servers[0].url = /api/v1/spellcheck` → Swagger UI ghép với
origin hiện tại, không cần biết domain thật.

> **Vì sao endpoint docs là public?** Swagger UI fetch spec bằng `fetch()` **không kèm**
> header `Authorization`. Nếu bảo vệ bằng JWT thì UI không tải được. Spec chỉ là mô tả
> API (không phải dữ liệu nghiệp vụ), và route gọi thật `/spellcheck/**` vẫn bắt buộc JWT.

## 5. Danh sách endpoint cho FE

Dựa trên `openapi.json` thật của Spellcheck service:

| Method | Đường dẫn qua core-service |
|---|---|
| GET | `/api/v1/spellcheck/api/health` |
| POST | `/api/v1/spellcheck/api/documents` (multipart) |
| GET | `/api/v1/spellcheck/api/jobs?limit&offset` |
| GET | `/api/v1/spellcheck/api/jobs/{jobId}` |
| DELETE | `/api/v1/spellcheck/api/jobs/{jobId}` |
| GET | `/api/v1/spellcheck/api/jobs/{jobId}/events` (SSE) |
| GET | `/api/v1/spellcheck/api/jobs/{jobId}/document` |
| GET | `/api/v1/spellcheck/api/jobs/{jobId}/result` |
| PATCH | `/api/v1/spellcheck/api/jobs/{jobId}/issues/{issueId}` |
| POST | `/api/v1/spellcheck/api/jobs/{jobId}/glossary` |
| POST | `/api/v1/spellcheck/api/jobs/{jobId}/passes` |
| POST | `/api/v1/spellcheck/api/jobs/{jobId}/finish` |
| POST | `/api/v1/spellcheck/api/jobs/{jobId}/export` |
| DELETE | `/api/v1/spellcheck/api/cache` |

## 6. Thêm một service mới

1. Thêm route trong `spring.cloud.gateway.server.webmvc.routes`:
   ```yaml
   - id: other-service-proxy
     uri: "http://<ip>:<port>"
     predicates:
       - Path=/other/**
     filters:
       - RewritePath=/api/v1/other/(?<seg>.*), /${seg}
   ```
2. Thêm `/other/**` vào `securityMatcher` + `requestMatchers` của `accessFilterChain`.
3. (Nếu muốn gộp docs) thêm block config + method trong `ExternalDocsService`
   và endpoint trong `ExternalDocsPublicController`, rồi thêm vào
   `springdoc.swagger-ui.urls`.
4. Nếu service đích cần biết user: forward thêm header từ JWT (`JwtUtil.getUserId()`),
   nhưng **không** forward nguyên token của core-service ra ngoài.

## 7. Kiểm chứng đã chạy thật

| Test | Kết quả |
|---|---|
| App khởi động | `Started Application in ~19s`, Tomcat 8080, context `/api/v1` |
| Gateway bean | `gatewayRouterFunctionHolder`, `gatewayCompositeRouterFunction`, `GatewayServerMvcAutoConfiguration` |
| Route bind | `spring.cloud.gateway.server.webmvc.routes[0].id/uri/predicates/filters` |
| Không JWT | `401` `{"status":1008,"message":"Unauthenticated"}` |
| Có JWT | `200` `{"status":"ok","llm":"model_not_found","database":"available","storage":"available"}` |
| Docs endpoint | `200`, `application/json;charset=UTF-8`, `servers[0].url=/api/v1/spellcheck`, 13 paths |
| Swagger config | dropdown có `Spellcheck Service (via core-service)` |

## 8. Tắt Gateway khi không cần

### Bẫy: `enabled=false` một mình sẽ làm app CHẾT

Trong jar 5.0.4, `GatewayServerMvcAutoConfiguration` có:

```java
@ConditionalOnProperty(name = "spring.cloud.gateway.server.webmvc.enabled",
                       matchIfMissing = true)
```

Property này **có thật** nhưng **không nằm trong `spring-configuration-metadata.json`**
(nên IDE không gợi ý). Và tắt nó **một mình** gây lỗi:

```
APPLICATION FAILED TO START
Parameter 0 of constructor in ...FilterAutoConfiguration$RetryFilterConfiguration
required a bean of type '...config.GatewayMvcProperties' that could not be found.
```

Vì 6 auto-config phụ **không** có cùng điều kiện `enabled` → chúng vẫn nạp và
đòi bean `GatewayMvcProperties` (do config chính tạo).

### Cách đúng: dùng profile `no-gateway`

File `core/src/main/resources/application-no-gateway.yml` đã tạo sẵn, exclude đủ 7 class:

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.cloud.gateway.server.mvc.GatewayServerMvcAutoConfiguration
      - org.springframework.cloud.gateway.server.mvc.GatewayMvcClassPathWarningAutoConfiguration
      - org.springframework.cloud.gateway.server.mvc.filter.FilterAutoConfiguration
      - org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctionAutoConfiguration
      - org.springframework.cloud.gateway.server.mvc.handler.GatewayMultipartAutoConfiguration
      - org.springframework.cloud.gateway.server.mvc.predicate.PredicateAutoConfiguration
      - org.springframework.cloud.gateway.server.mvc.config.DefaultFunctionConfiguration
```

Chạy:

```bash
./mvnw spring-boot:run -pl core -Dspring-boot.run.profiles=no-gateway
# hoặc
java -jar core-1.0-SNAPSHOT.jar --spring.profiles.active=no-gateway
# hoặc
SPRING_PROFILES_ACTIVE=no-gateway
```

### Hệ quả khi tắt (đã test thật)

| Mục | Trước | Sau khi tắt |
|---|---|---|
| Khởi động | OK | OK (`Started Application in 19.4s`, profile `no-gateway` active) |
| Gateway bean | `gatewayRouterFunctionHolder`, `gatewayCompositeRouterFunction` | **Không còn** (chỉ còn `routerFunctionMapping` / `routerFunctionProvider` của Spring MVC core) |
| Tổng bean | 869 | 828 (giảm 41) |
| `GET /api/v1/spellcheck/api/health` (có JWT) | `200` | `500` (UNEXPECTED — `NoResourceFoundException` bị `GlobalExceptionHandler` bắt) |
| `GET /api/v1/public/docs/spellcheck` | `200` | `502` (upstream docs không reachable lúc test; endpoint vẫn tồn tại vì không phụ thuộc gateway) |
| Swagger dropdown | có spellcheck | vẫn còn (bỏ comment trong profile để ẩn) |

> Block `spring.cloud.gateway.*` trong `application.yml` **không cần xóa** khi tắt —
> không có auto-config nào đọc nên bị bỏ qua hoàn toàn.

### Nếu muốn xóa hẳn (không chỉ tắt)

Code Java **không** import gì của Spring Cloud (đã quét `core/src/main/java` — 0 match).
Nên có thể xóa được: dependency `spring-cloud-starter-gateway-server-webmvc`,
`spring-cloud.version` + `dependencyManagement` trong `pom.xml`, block
`spring.cloud.gateway.*` và profile `no-gateway`. Nhưng khi đó `/spellcheck/**`
sẽ mất hoàn toàn → phải bỏ luôn mục trong `springdoc.swagger-ui.urls` và
`ExternalDocsPublicController` (nếu không dùng nữa).

