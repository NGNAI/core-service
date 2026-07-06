# Core Module Instructions

## Overview
The core module is the main application module containing all business logic, REST endpoints, and integration points. It serves as the heart of the application.

## Package Structure
```
core/
├── controller/     (REST endpoints)
├── service/        (Business logic)
├── repository/     (Data access layer)
├── entity/         (JPA entities)
│   ├── postgres/   (Standard JPA entities)
│   └── embeddable/ (JPA @Embeddable classes)
├── dto/            (Data transfer objects)
│   ├── own/        (Internal DTOs)
│   │   ├── request/   (Incoming request payloads)
│   │   └── response/  (Outgoing response payloads)
│   └── outer/      (External integration DTOs)
│       ├── ingestion/
│       ├── otp/
│       └── rag/
├── mapper/         (Object mapping with MapStruct)
├── aspect/         (Cross-cutting concerns)
├── security/       (Security components)
├── config/         (Configuration classes)
├── util/           (Utility classes)
└── CoreApplication.java (Main application class)
```

## Key Components

### Controller Layer
- REST endpoints for API exposure
- Request/response handling
- Exception handling
- Security annotations

### Service Layer
- Business logic implementation
- Transaction management
- Service-to-service communication
- Integration with repositories

### Repository Layer
- Data access operations
- JPA repositories
- Custom query methods
- Database interaction

### Entity Layer
- JPA entities
- Database schema mapping
- Relationships between entities
- Entity lifecycle management

### DTO Layer
- Data transfer objects
- Request/response models
- Data validation
- Separation of domain and presentation layers

### Mapper Layer
- Object mapping with MapStruct
- Conversion between entities and DTOs
- Custom mapping logic

### Security Layer
- Authentication and authorization
- JWT handling
- OAuth2 integration
- Role-based access control

### Configuration
- Application configuration
- Security configuration
- Database configuration
- Redis configuration
- MinIO configuration

## Key Dependencies
- Spring Web, Data JPA, Security, Batch, Validation
- Spring Boot Admin Client
- PostgreSQL driver
- Redis starter
- MinIO client
- Springdoc OpenAPI
- MapStruct
- Lombok

## Integration Points
1. **Database**: PostgreSQL with JPA/Hibernate
2. **Caching**: Redis
3. **Storage**: MinIO object storage
4. **Monitoring**: Spring Boot Admin
5. **Documentation**: OpenAPI/Swagger
6. **Security**: OAuth2 Resource Server with JWT