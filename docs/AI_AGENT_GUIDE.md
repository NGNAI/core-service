# AI Agent Guide: NGNAI Core Service Project

## 🎯 Purpose
This guide gives an AI Agent (e.g., GitHub Copilot, Cursor, or a custom agent) a concise, deep understanding of the `core-service` repository. It covers the overall architecture, module responsibilities, package layout, and the conventions that must be followed when adding or modifying code.

## 🏗️ Project Architecture
The project is a **multi‑module Maven** Spring Boot application that follows **Clean Architecture** principles.

### Modules
| Module | Responsibility |
|--------|----------------|
| `core/` | Main application – business logic, REST APIs, security, persistence, and integrations (PostgreSQL, Redis, MinIO). |
| `common/` | Shared utilities, constants, and reusable components used across all modules. |
| `admin-server/` | Spring Boot Admin server for monitoring the health and metrics of the `core` application. |

### Core Architectural Patterns
* **Layered Architecture** – `Controller → Service → Repository → Entity`. 
* **DTO Pattern** – Separate API request/response models (`dto`) from domain entities (`entity`). 
* **MapStruct Mapping** – All conversions between DTOs and entities are handled by MapStruct mappers. 
* **Security** – OAuth2 Resource Server with JWT, role‑based access control (`@PreAuthorize`). 
* **Persistence** – Spring Data JPA with PostgreSQL, Flyway migrations, and Redis caching. 

## 📂 Key Package Deep‑Dive (`core/src/main/java/ai/`)
When adding new code, you **must** respect this granular structure:

### 1. API & Communication
* `controller/` – REST endpoints. Sub‑packages like `admin/` exist for specific scopes.
* `dto/`
  * `own/` – Internal DTOs used within the service.
    * `request/` – Incoming request payloads.
    * `response/` – Outgoing response payloads.
  * `outer/` – DTOs for external integrations (e.g., `ingestion`, `otp`, `rag`).

### 2. Business Logic & Data
* `service/` – Business logic implementation.
* `repository/` – Spring Data JPA repositories.
* `entity/`
  * `postgres/` – Standard JPA entities.
  * `embeddable/` – JPA `@Embeddable` classes.
* `mapper/` – MapStruct mappers.

### 3. Cross‑Cutting Concerns
* `aspect/` – AOP implementations (logging, auditing, etc.).
* `security/` – Security filters, JWT logic, and RBAC.
* `config/` – Spring `@Configuration` classes.
* `exception/` – Custom business exceptions.
* `annotation/` – Custom annotations and their validators.

## 🛠️ How to Use This Guide
* **When starting a new task** – Read the relevant section to understand which module and package the task belongs to.
* **When implementing a feature** – Refer to the `docs/skills/` directory for specialized instructions (e.g., adding an API, creating an entity).
* **When refactoring** – Ensure you follow the `coding-conventions.instructions.md` and the package structure defined here.

## 🚀 Quick Reference: Build & Run
```bash
mvn clean install          # Build the entire project
mvn spring-boot:run -pl core   # Run the core module
```
