# Skill: Add API Endpoint

## Purpose
Guides the AI Agent on how to add a new REST endpoint to the `core` module following the established architecture and conventions.

## Prerequisites
* The project uses **Spring Boot 4.0.3** with **Java 25**.
* All DTOs are defined under `ai/dto/own/request` and `ai/dto/own/response`.
* Mapping between DTOs and entities is handled by MapStruct mappers in `ai/mapper`.
* Services are located in `ai/service` and repositories in `ai/repository`.
* Security is enforced via `@PreAuthorize` annotations.

## Steps
1. **Create DTOs**
   * Add a request DTO in `ai/dto/own/request`.
   * Add a response DTO in `ai/dto/own/response`.
   * Annotate with `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` (Lombok).
   * Add validation annotations (`@NotNull`, `@Size`, etc.) as needed.

2. **Create Mapper**
   * Add a MapStruct mapper interface in `ai/mapper`.
   * Define mapping methods between request/response DTOs and the corresponding entity.
   * Use `@Mapper(componentModel = "spring")`.

3. **Create Service Method**
   * Add a method in the appropriate service class (e.g., `UserService`).
   * Inject the repository and mapper via constructor.
   * Implement business logic, transaction handling, and exception mapping.

4. **Create Controller Method**
   * Add a method in the relevant controller (e.g., `UserController`).
   * Annotate with `@PostMapping`, `@RequestBody`, and `@Valid`.
   * Use `@PreAuthorize("hasRole('ADMIN')")` if needed.
   * Return `ResponseEntity<YourResponseDto>`.

5. **Update OpenAPI**
   * Add `@Operation` and `@ApiResponse` annotations for Swagger documentation.

6. **Add Tests**
   * Create unit tests for the service using JUnit 5 and Mockito.
   * Create integration tests for the controller using `@SpringBootTest`.

## Example
```java
// Request DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    @NotBlank
    private String username;
    @Email
    private String email;
}

// Response DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserResponse {
    private Long id;
    private String username;
}

// Mapper
@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(CreateUserRequest dto);
    CreateUserResponse toDto(User entity);
}

// Service
@Service
public class UserService {
    private final UserRepository repo;
    private final UserMapper mapper;
    public UserService(UserRepository repo, UserMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest req) {
        User user = mapper.toEntity(req);
        User saved = repo.save(user);
        return mapper.toDto(saved);
    }
}

// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;
    public UserController(UserService service) {
        this.service = service;
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateUserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.ok(service.createUser(req));
    }
}
```

## Notes
* Keep DTOs lightweight; avoid exposing internal entity fields.
* Use `@Transactional` only on service methods that modify data.
* Ensure proper exception handling; map domain exceptions to HTTP status codes.
