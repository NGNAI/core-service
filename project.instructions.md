# Core Service Project Instructions

## Project Overview
This is a multi-module Spring Boot application built with Maven. The project follows a clean architecture pattern with clear separation of concerns between modules.

## Project Structure
```
core-service/
├── pom.xml (parent)
├── core/     (main application module)
├── common/   (shared components)
└── admin-server/ (Spring Boot Admin server)
```

## Modules

### Core Module
- Main application module containing business logic
- Dependencies: Spring Web, Data JPA, Security, Batch, Validation
- Integration with Spring Admin for monitoring
- OpenAPI/Swagger documentation
- OAuth2 Resource Server for authentication
- PostgreSQL database integration
- Redis caching integration
- MinIO object storage integration

### Common Module
- Shared components and utilities
- Reusable code across other modules

### Admin Server Module
- Spring Boot Admin server for monitoring applications
- Provides UI to monitor Spring Boot applications
- Security protection for admin endpoints

## Key Technologies
- **Spring Boot 4.0.3** with Java 25
- **Maven** for build management
- **PostgreSQL** as primary database
- **Redis** for caching
- **MinIO** for object storage
- **Spring Security** with OAuth2 and JWT
- **MapStruct** for object mapping
- **Lombok** for reducing boilerplate code
- **Springdoc OpenAPI** for API documentation

## Build Commands
```bash
# Build entire project
mvn clean install

# Build specific module
mvn clean install -pl core

# Run main application
mvn spring-boot:run

# Run specific module
mvn spring-boot:run -pl core
```

## Development Conventions
- Clean architecture with separation of concerns
- Package structure follows: controller, service, repository, entity, dto, mapper, aspect, security, util
- Lombok annotations for reducing boilerplate
- MapStruct for object mapping
- Spring Boot DevTools for development
- Flyway for database migrations

## Security Architecture
- JWT-based authentication
- OAuth2 Resource Server integration
- Role-based access control
- Spring Security configuration

## Monitoring
- Spring Boot Admin integration
- Actuator endpoints for health and metrics
- Application monitoring via UI