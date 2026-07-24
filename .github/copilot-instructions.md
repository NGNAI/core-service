# Copilot Instructions — core-service

Hướng dẫn cho GitHub Copilot khi làm việc trong repo `core-service`.

## Ngôn ngữ phản hồi
Luôn phản hồi bằng **tiếng Việt** khi giải thích, gợi ý code, hoặc hỗ trợ bất kỳ hình thức nào cho repo này. Code comments và documentation cũng nên bằng tiếng Việt khi phù hợp.

## Tech stack
- Spring Boot 4.0.6 + Java 25 (Temurin 25)
- Maven multi-module (core, common, admin-server)
- PostgreSQL + Flyway + Redis + MinIO
- Spring Security OAuth2 Resource Server + JWT (Nimbus)
- MapStruct + Lombok (annotation processing)
- Springdoc OpenAPI

## Build
```bash
./mvnw clean install -DskipTests   # build full
./mvnw clean compile -pl core -am   # compile core
```

## Conventions chính
- **Lombok:** `@RequiredArgsConstructor` + `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)` cho service/controller
- **Mapper:** MapStruct `@Mapper(componentModel = "spring")` extends `GeneralMapper`
- **Security:** `@PreAuthorize("@perm.canAccess(...)")` SpEL cho RBAC
- **Audit:** `@Audited(action, resource, resourceIdExpression, description)` annotation
- **Response:** `ApiResponseModel<T>` wrapper; list dùng `CustomPairModel<Long, List<T>>`
- **Pagination:** `PageableFilterDto` (pageNumber/pageSize/sortBy/sortDir)
- **Entity audit:** `AuditEmbed` (createdAt/createdBy/updatedAt/updatedBy) qua Spring Data JPA Auditing

## Package structure
```
ai/
  controller/     REST (admin/, user/, + public)
  service/        Business logic (+ api/, dashboard/, report/)
  repository/     JPA
  entity/postgres/  JPA entities (+ embeddable/)
  dto/own/        request/ (+ filter/), response/
  dto/outer/      integration DTOs (ingestion, otp, rag)
  mapper/         MapStruct
  security/       filters, SecurityConfig, JwtDecoder
  aspect/         AuditAspect
  enums/          enums + ApiResponseStatus
  util/           JwtUtil, ServletUtil, ...
```

## Khi sửa code
- **Không sửa** method read cũ khi thêm public access — thêm overload `*Shared` (bỏ ownership check)
- **Không revert** `core/pom.xml` `<source>` về `1.8` — phải dùng `${java.version}` (25)
- Thêm error codes vào `ApiResponseStatus` theo dải số, có comment section header
- Thêm Flyway migration (`V{n}__desc.sql`) nếu tạo bảng mới
- Cập nhật `AGENTS.md` và `docs/` khi thêm tính năng lớn

## Tính năng hiện có
Auth (local+LDAP), RBAC, audit log, Topic/Notebook (RAG chat SSE), Note, Draft (AI+version), Data ingestion, Dashboard, Reports, System settings, System health, LDAP, **Share link public** (Topic/Notebook read-only).

Xem `AGENTS.md` và `docs/` để biết chi tiết.