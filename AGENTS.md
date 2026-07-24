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
- **ddl-auto:** `update` (Hibernate tự sinh schema), nhưng vẫn dùng Flyway cho migrations có control

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
- Data ingestion (upload, folder, MinIO, ingestion pipeline, scheduler)
- Dashboard (admin global + user personal), Reports (activity/user/data/comprehensive + CSV export)
- System settings (admin + public), System health, LDAP import/sync
- **Share link public** (Topic/Notebook read-only) — xem `docs/share-link-feature.md`

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