# Multi-stage build for smaller image size
FROM gradle:9.3-jdk21-alpine AS builder

WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle.properties ./

# Copy source code
COPY src ./src
COPY clients ./clients

# Build the application
RUN gradle build -x test --no-daemon

# Create data directory for SQLite (will be copied to runtime)
RUN mkdir -p /app/data

# Runtime stage - Google Distroless (hardened, no shell, minimal attack surface)
FROM gcr.io/distroless/java21-debian12:nonroot

LABEL maintainer="pcsalt"
LABEL description="Log Collector - Centralized logging service"
LABEL version="1.0.0"

WORKDIR /app

# Copy JAR from builder (nonroot image runs as uid 65532)
COPY --from=builder --chown=65532:65532 /app/build/libs/log-collector.jar /app/log-collector.jar

# Copy data directory with correct ownership for SQLite
COPY --from=builder --chown=65532:65532 /app/data /app/data

# Environment variables with defaults
ENV SERVER_PORT=7777 \
    DATABASE_PATH=/app/data/logs.db \
    LOG_RETENTION_HOURS=24 \
    LOG_RETENTION_CRON="0 0 * * * *" \
    CLEANUP_ENABLED=true

# Expose port
EXPOSE 7777

# Distroless has no shell, so no wget for health check
# Health check via Docker Compose or K8s liveness probe on /actuator/health

# Run the application (exec form required - no shell in distroless)
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "/app/log-collector.jar"]
