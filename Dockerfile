# Multi-stage build for smaller image size
FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle.properties ./

# Copy source code
COPY src ./src
COPY clients ./clients

# Build the application
RUN gradle build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="pcsalt"
LABEL description="Log Collector - Centralized logging service"
LABEL version="1.0.0"

# Create app user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Create data directory for database
RUN mkdir -p /app/data && chown -R appuser:appgroup /app

# Copy JAR from builder
COPY --from=builder /app/build/libs/log-collector.jar /app/log-collector.jar

# Change to non-root user
USER appuser

# Environment variables with defaults
ENV SERVER_PORT=7777 \
    DATABASE_PATH=/app/data/logs.db \
    LOG_RETENTION_HOURS=24 \
    LOG_RETENTION_CRON="0 0 * * * *" \
    CLEANUP_ENABLED=true \
    JAVA_OPTS="-Xms256m -Xmx512m"

# Expose port
EXPOSE ${SERVER_PORT}

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/log-collector.jar"]
