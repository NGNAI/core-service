# Build stage
FROM maven:3.9.9-eclipse-temurin-25 AS build
WORKDIR /build

# Copy parent pom.xml first to leverage Maven layer caching
COPY pom.xml ./
COPY common/pom.xml common/
COPY core/pom.xml core/

# Download dependencies
RUN mvn -B -q dependency:go-offline -pl core -am

# Copy source code
COPY common/src common/src
COPY core/src core/src

# Build the application (skip tests)
RUN mvn -B -pl core -am clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Install tzdata for timezone support
RUN apk add --no-cache tzdata

# Set timezone (optional, change to your preferred timezone)
ENV TZ=Asia/Ho_Chi_Minh

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S -G appgroup appuser

# Copy the JAR file from build stage
COPY --from=build /build/core/target/core-1.0-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/actuator/health/readiness || exit 1

# Switch to non-root user
USER appuser

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
