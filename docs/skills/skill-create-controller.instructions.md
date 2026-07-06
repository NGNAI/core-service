# Skill: Create REST Controller

## Purpose
Provides a step‑by‑step guide for adding a new REST controller to the `core` module while respecting the project's conventions.

## Prerequisites
* The project uses **Spring Boot 4.0.3** with **Java 25**.
* Controllers are located in `ai/controller`.
* DTOs are in `ai/dto/own`.
* Services are in `ai/service`.
* Security is enforced via `@PreAuthorize` annotations.

## Steps
1. **Create Controller Class**
   * Place in `ai/controller` (or a sub‑package if it belongs to a specific domain).
   * Annotate with `@RestController` and `@RequestMapping("/api/<resource>")`.
   * Inject the relevant service via constructor.

2. **Add Endpoint Methods**
   * Use `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` as appropriate.
   * Annotate method parameters with `@PathVariable`, `@RequestBody`, `@RequestParam`.
   * Validate request bodies with `@Valid`.
   * Apply `@PreAuthorize` for role‑based access.
   * Return `ResponseEntity<YourDto>`.

3. **Document with OpenAPI**
   * Add `@Operation` and `@ApiResponse` annotations for Swagger documentation.

4. **Add Tests**
   * Create integration tests using `@SpringBootTest` and `MockMvc`.
   * Verify status codes, response bodies, and security constraints.

## Example
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;
    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(service.getUserById(id));
    }
}
```

## Notes
* Keep controller logic thin – delegate business logic to services.
* Use DTOs for request/response; never expose entity objects directly.
* Ensure proper exception handling; map domain exceptions to HTTP status codes.
