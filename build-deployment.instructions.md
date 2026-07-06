# Build and Deployment Instructions

## Build Process

### Prerequisites
- Java 25
- Maven 3.8+
- PostgreSQL database
- Redis server
- Docker (for containerized deployment)

### Local Development Build
```bash
# Clean and build the entire project
mvn clean install

# Build specific module
mvn clean install -pl core

# Run the main application
mvn spring-boot:run

# Run specific module
mvn spring-boot:run -pl core

# Build with specific profile
mvn clean install -Pdev
```

### Build Profiles
- **dev**: Development profile with local configurations
- **test**: Testing profile with test configurations
- **prod**: Production profile with production configurations

### Maven Commands
```bash
# Compile project
mvn compile

# Run tests
mvn test

# Package application
mvn package

# Generate Javadoc
mvn javadoc:javadoc

# Generate site
mvn site
```

## Deployment Strategies

### Local Deployment
1. Start PostgreSQL database
2. Start Redis server
3. Run application with:
   ```bash
   mvn spring-boot:run
   ```
4. Access application at http://localhost:8080

### Docker Deployment
```dockerfile
# Dockerfile for core module
FROM openjdk:25-jdk-slim
VOLUME /tmp
COPY target/core-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

```bash
# Build Docker image
mvn clean package docker:build

# Run Docker container
docker run -p 8080:8080 core-service:latest
```

### Production Deployment
1. Build with production profile:
   ```bash
   mvn clean install -Pprod
   ```
2. Deploy JAR file to production server
3. Configure environment variables
4. Start application with:
   ```bash
   java -jar core-1.0-SNAPSHOT.jar
   ```

## Environment Configuration

### Application Properties
The application uses `application.yml` with environment-specific profiles:
- `application-dev.yml` - Development
- `application-test.yml` - Testing
- `application-prod.yml` - Production

### Environment Variables
```bash
# Database configuration
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/core_db
export SPRING_DATASOURCE_USERNAME=core_user
export SPRING_DATASOURCE_PASSWORD=core_password

# Redis configuration
export SPRING_REDIS_HOST=localhost
export SPRING_REDIS_PORT=6379

# Security configuration
export JWT_SECRET=your_jwt_secret_key
export OAUTH2_CLIENT_ID=your_client_id
export OAUTH2_CLIENT_SECRET=your_client_secret
```

## Monitoring and Health Checks

### Actuator Endpoints
The application exposes Actuator endpoints for monitoring:
- `/actuator/health` - Application health
- `/actuator/info` - Application info
- `/actuator/metrics` - Metrics
- `/actuator/beans` - Spring beans
- `/actuator/env` - Environment properties

### Spring Boot Admin
The admin server provides:
- Application status monitoring
- Health checks
- Metrics collection
- Log viewing
- Thread dumps

## CI/CD Pipeline

### Build Pipeline
1. Code checkout
2. Run tests
3. Build artifacts
4. Run integration tests
5. Build Docker images
6. Push to registry
7. Deploy to staging/production

### Deployment Pipeline
1. Pull latest images
2. Stop old containers
3. Start new containers
4. Run health checks
5. Update load balancer

## Troubleshooting

### Common Issues
1. **Database connection issues**: Check database URL, username, and password
2. **Port conflicts**: Ensure port 8080 is available
3. **Missing dependencies**: Run `mvn clean install` to resolve dependencies
4. **Security issues**: Verify JWT secret and OAuth2 configurations

### Logging
- Application logs are written to console by default
- Configure log file output in application properties
- Use appropriate log levels (DEBUG, INFO, WARN, ERROR)
- Implement structured logging for better monitoring