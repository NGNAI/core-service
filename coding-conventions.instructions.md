# Coding Conventions and Best Practices

## General Principles
- Follow clean architecture principles with clear separation of concerns
- Maintain consistent code style throughout the project
- Use meaningful names for variables, methods, and classes
- Write self-documenting code with appropriate comments
- Follow SOLID principles and design patterns where applicable

## Java Conventions

### Naming Conventions
- **Classes**: PascalCase (e.g., UserService, UserEntity)
- **Methods**: camelCase (e.g., getUserById, saveUser)
- **Variables**: camelCase (e.g., userName, userId)
- **Constants**: UPPER_SNAKE_CASE (e.g., MAX_RETRY_COUNT)
- **Packages**: lowercase (e.g., com.example.core.service)

### Code Structure
- Use Lombok annotations to reduce boilerplate code
- Prefer constructor injection over field injection
- Use MapStruct for object mapping
- Follow the repository pattern for data access
- Implement proper exception handling

### Documentation
- Use JavaDoc for all public classes and methods
- Document complex business logic
- Include parameter and return value descriptions
- Use @since tags for new features

## Spring Boot Conventions

### Controllers
- Use @RestController annotation
- Implement proper HTTP status codes
- Use @PathVariable, @RequestParam, @RequestBody appropriately
- Include proper validation annotations
- Handle exceptions with @ExceptionHandler or GlobalExceptionHandler

### Services
- Use @Service annotation
- Implement business logic in service layer
- Use @Transactional annotation for database operations
- Inject dependencies via constructor
- Separate concerns between different service classes

### Repositories
- Use @Repository annotation
- Extend appropriate Spring Data interfaces (JpaRepository, etc.)
- Use method naming conventions for queries
- Implement custom queries with @Query annotation when needed

### Entities
- Use @Entity annotation
- Follow JPA conventions for relationships
- Use @Id for primary keys
- Use appropriate column annotations
- Implement proper relationships (OneToOne, ManyToOne, etc.)

## Security Conventions

### Authentication
- Use JWT-based authentication
- Implement OAuth2 Resource Server
- Follow role-based access control
- Use @PreAuthorize and @PostAuthorize annotations
- Implement proper token validation

### Authorization
- Define roles and permissions clearly
- Use Spring Security configuration
- Implement custom access control logic when needed
- Secure endpoints appropriately

## Testing Conventions

### Unit Tests
- Use JUnit 5
- Use Mockito for mocking
- Test business logic in service layer
- Test edge cases and error conditions
- Use @BeforeEach for test setup

### Integration Tests
- Use @SpringBootTest annotation
- Test complete flow from controller to database
- Use @AutoConfigureTestDatabase for test databases
- Test security aspects

## Code Quality

### Code Style
- Follow Google Java Style Guide
- Use consistent indentation (4 spaces)
- Keep lines under 120 characters
- Use meaningful variable names
- Avoid code duplication

### Performance
- Use proper indexing in database
- Implement caching where appropriate (Redis)
- Optimize database queries
- Use lazy loading for relationships when appropriate

### Error Handling
- Use custom exceptions for business logic
- Implement global exception handler
- Return appropriate HTTP status codes
- Log errors appropriately
- Don't expose sensitive information in error messages

## Build and Deployment

### Maven
- Use proper dependency management
- Follow versioning conventions
- Use profiles for different environments
- Keep dependencies up to date
- Use proper scope for dependencies

### Configuration
- Use application.yml for configuration
- Separate environment-specific configurations
- Use @Value annotation for configuration properties
- Implement proper configuration validation