# Skill: Create JPA Entity

## Purpose
Provides a step‑by‑step guide for adding a new JPA entity, its repository, mapper, and DTOs while respecting the project's conventions.

## Prerequisites
* The project uses **Spring Data JPA** with **PostgreSQL**.
* Entities are located in `ai/entity/postgres`.
* Repositories extend `JpaRepository` and are in `ai/repository`.
* Mapping is handled by MapStruct mappers in `ai/mapper`.
* DTOs are in `ai/dto/own`.

## Steps
1. **Create Entity Class**
   * Place in `ai/entity/postgres`.
   * Annotate with `@Entity`, `@Table(name = "<table_name>")`.
   * Use Lombok (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`).
   * Define fields with appropriate JPA annotations (`@Id`, `@GeneratedValue`, `@Column`).
   * Add relationships (`@OneToMany`, `@ManyToOne`, etc.) if needed.

2. **Create Repository Interface**
   * Place in `ai/repository`.
   * Extend `JpaRepository<YourEntity, Long>`.
   * Add custom query methods if required.

3. **Create Mapper**
   * Place in `ai/mapper`.
   * Use `@Mapper(componentModel = "spring")`.
   * Map between entity and DTOs.

4. **Create DTOs**
   * Request DTO in `ai/dto/own/request`.
   * Response DTO in `ai/dto/own/response`.
   * Use Lombok and validation annotations.

5. **Add Service Method**
   * Inject repository and mapper.
   * Implement CRUD operations with `@Transactional` where appropriate.

6. **Add Controller Endpoints**
   * Create REST endpoints in the relevant controller.
   * Use `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`.
   * Apply security annotations (`@PreAuthorize`).

7. **Add Tests**
   * Unit tests for service and repository.
   * Integration tests for controller.

## Example
```java
// Entity
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String email;
}

// Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}

// Mapper
@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(CreateUserRequest dto);
    CreateUserResponse toDto(User entity);
}

// DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    @NotBlank
    private String username;
    @Email
    private String email;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserResponse {
    private Long id;
    private String username;
}
