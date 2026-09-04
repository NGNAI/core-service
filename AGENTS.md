# AGENTS.md — core-service

Hướng dẫn cho AI agents làm việc trong repo `core-service`.

## Tech stack
- **Spring Boot 4.0.6** + **Java 25** (Temurin 25)
- **Maven** multi-module: `core` (main), `common` (shared), `admin-server` (Spring Boot Admin)
- **PostgreSQL** + Flyway migrations (`core/src/main/resources/db/migration/`)
- **Redis** cache, **MinIO** object storage
- **Spring Security** OAuth2 Resource Server + JWT (Nimbus JOSE)
- **MapStruct** + **Lombok** (annotation processing qua maven-compiler-plugin)
- **Springdoc OpenAPI** cho API docs

## Build & run
```bash
# Build full (annotation processing cần chạy đầy đủ)
./mvnw clean install -DskipTests

# Compile chỉ core module
./mvnw clean compile -pl core -am

# Run
./mvnw spring-boot:run -pl core
```

> **Lưu ý pom.xml:** `core/pom.xml` dùng `maven-compiler-plugin` với `<source>${java.version}</source>` (25) và annotationProcessorPaths cho Lombok + MapStruct. Không revert về `<source>1.8</source>`.

## Coding conventions
- **Package:** `ai.{controller,service,repository,entity,dto,mapper,security,aspect,configuration,util,enums}`
- **DTO:** `ai.dto.own.request` (incoming), `ai.dto.own.response` (outgoing), `ai.dto.own.request.filter` (filter DTO extends `PageableFilterDto`)
- **Entity:** `ai.entity.postgres` (JPA), `ai.entity.postgres.embeddable` (`AuditEmbed`)
- **Lombok:** `@RequiredArgsConstructor` + `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)` cho service/controller
- **Mapper:** MapStruct `@Mapper(componentModel = "spring")` extends `GeneralMapper` (helper audit fields)
- **Security:** `@PreAuthorize("@perm.canAccess(...)")` SpEL cho RBAC; `@PreAuthorize("@adminAccessGuard.isAllowed()")` cho whitelist
- **Audit:** `@Audited(action, resource, resourceIdExpression, description)` annotation → `AuditAspect` ghi log async
- **Response:** `ApiResponseModel<T>` wrapper (status, message, count, data)
- **Pagination:** `PageableFilterDto` (pageNumber, pageSize, sortBy, sortDir), `CustomPairModel<Long, List<>>` cho list response
- **Schema DB:** Flyway là nguồn quản lý DUY NHẤT — `ddl-auto: none`. `V1__init_schema.sql` chứa CREATE TABLE đầy đủ; mọi thay đổi schema mới phải thêm migration `V{n}__desc.sql`. KHÔNG revert `ddl-auto` về `update`

## Security architecture
- **Filter chains (SecurityConfig):**
  - `@Order(0)` `/public/**` — permitAll (share link public, settings public). `ShareLinkAuthFilter` xác thực token.
  - `@Order(1)` `/actuator/**` — health/info permitAll, còn lại basic auth
  - `@Order(2)` `/auth/**` — login/introspect permitAll, select-org authenticated
  - `@Order(3)` `/admin/**`, `/user/**`, `/category/**` — authenticated (JWT)
- **JWT:** `CustomJwtDecoder` verify token qua `AuthService#introspect`. Claims: `user_id`, `org_id`, `sub` (username), `token_type`
- **JwtUtil:** thread-local `SecurityContextHolder` → `getUserId()`, `getOrgId()`, `getUserName()`
- **API Key filters:** `DataIngestionApiKeyFilter`, `AttachmentApiKeyFilter` — cho integration endpoints (webhook, presigned)
- **Account lock:** `UserEntity.loginAttempts` + `lockedUntil` (config `security.maxLoginAttempts`, `security.accountLockDuration`)

## Key patterns
- **Ownership check:** `TopicService.validateTopicOfUser(topicId, userId)`, `NoteBookService.validateNoteBookOfUser(...)` — throw `PERMISSION_DENIED` nếu không phải owner
- **Shared methods (no ownership):** thêm overload `*Shared` (vd `getEntityByIdShared`, `getSourcesShared`) cho public access flow — **không sửa** method cũ
- **Source flow:** Topic/Notebook source → MinIO upload → ingestion service (vector) → callback/poll → `SystemEventType` SSE event
- **Delete queue:** `deleteStatus` (ACTIVE/PENDING_DELETE/DELETE_FAILED) + scheduler retry
- **Auto-ingestion retry:** `DataIngestionAutoImportScheduler` quét thư mục input → move sang `.processing` → `ingestLocalFile` → move về input (retry) hoặc sang `.failed` (dừng hẳn). Khi đẩy RAG FAILED, record FAILED cũ được **tái sử dụng** (tìm theo `name + parent + owner + org + fromSource + accessLevel`) để **tránh trùng lắp DB record**; retry đếm qua `retryCount` trong entity, giới hạn bởi config `auto-ingestion.max-retry-attempts` (mặc định 3, đặt 0 để không retry).
- **Auto-ingestion allowed extensions:** file có extension không nằm trong `auto-ingestion.allowed-extensions` (mặc định txt/pdf/docx/html/htm/md/csv) bị **bỏ qua hoàn toàn ngay trong filter** (không move, không tạo record DB, không gọi RAG) để tránh loop lỗi với file RAG không hỗ trợ (vd `.DS_Store`).

## Secrets & cấu hình môi trường
- `application.yml` dùng placeholder `${ENV_VAR:default}` cho secrets: `DB_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET_KEY`, `OTP_API_KEY`, `MINIO_ACCESS_KEY/SECRET_KEY`, `ATTACHMENT_API_KEY`, `DATA_INGESTION_API_KEY`, callback secrets, `LOKI_PASSWORD`, `ACTUATOR_USER_PASSWORD`.
- Mặc định giữ giá trị dev cũ để không phá local flow; **production bắt buộc đặt env riêng**.
- CORS: `security.allowed-origins` (AppProperties.Security) — rỗng = cho phép tất cả (dev); production liệt kê domain FE (hỗ trợ wildcard pattern).

## Modules
- `core/` — toàn bộ business logic, REST API
- `common/` — shared util (`ai.util`)
- `admin-server/` — Spring Boot Admin server (monitoring)

## Testing
- Hiện chưa có unit test convention rõ ràng. Manual test qua Postman/Swagger UI (`/swagger-ui/**`)
- Swagger UI public: `/swagger-ui/**`, `/v3/api-docs/**` (permitAll)

## Tính năng đã có
- Auth (local + LDAP/OTP), RBAC (org/role/permission), audit log
- Topic/Notebook (CRUD, sources, RAG chat SSE), Note, Draft (AI soạn thảo + version)
- Data ingestion (upload, folder, MinIO, ingestion pipeline, scheduler, **auto-import từ thư mục với retry tự động**)
- Dashboard (admin global + user personal), Reports (activity/user/data/comprehensive + CSV export)
- System settings (admin + public), System health, LDAP import/sync
- **Share link public** (Topic/Notebook read-only) — xem `docs/share-link-feature.md`
- **Quick Prompt Template** (prompt mẫu cho chat Topic/NotebookLM — SYSTEM global + USER cá nhân) — xem `docs/prompt-template-feature.md`

## Monitoring (Prometheus + Grafana + Loki + Alertmanager)
- Stack chạy qua `monitoring/docker-compose.yml` (app + metrics + logs + alerts)
- Prometheus scrape app qua `host.docker.internal:${PROMETHEUS_APP_PORT:-8080}`, basic auth `monitor/changeme` (dev)
- Alert rules Prometheus: `monitoring/prometheus/rules/core-service.yml` (app down, 5xx, p95, heap)
- Alertmanager: `monitoring/alertmanager/alertmanager.yml` (receivers TODO khi cần email/Slack)
- Grafana alert qua UI (provisioning file không hoạt động trên Grafana 13)

## Khi thêm tính năng mới
1. Thêm error codes vào `ApiResponseStatus` (theo dải số tương ứng, comment section header)
2. Thêm enum nếu cần (`ai.enums`)
3. Tạo entity + repository (ddl-auto=update tự sinh, nhưng thêm Flyway migration nếu cần control)
4. Tạo DTOs (`ai.dto.own.request/response`)
5. Tạo mapper (MapStruct, extends `GeneralMapper`)
6. Tạo service (Lombok `@RequiredArgsConstructor`, `@FieldDefaults PRIVATE FINAL`)
7. Tạo controller (`/admin/*` hoặc `/user/*` hoặc `/public/*`)
8. Decorate service methods với `@Audited` nếu cần audit
9. Cập nhật `SecurityConfig` nếu thêm path pattern mới
10. Cập nhật docs (`docs/` + file này nếu cần)