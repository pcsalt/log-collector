# Log Collector Client

HTTP Logback appender for sending logs to the log-collector service.

## Installation

### Option A: Copy-Paste (Recommended for now)

Copy these files to your project:

```
src/main/kotlin/com/pcsalt/logcollector/client/
├── HttpLogAppender.kt      # Batched appender (recommended)
└── AsyncHttpLogAppender.kt # Simple async appender
```

### Option B: Maven/Gradle Dependency (Future)

```kotlin
// build.gradle.kts
dependencies {
  implementation("com.pcsalt:log-collector-client:1.0.0")
}
```

## Usage

### 1. Add to logback-spring.xml

```xml
<configuration>
  <!-- Define properties -->
  <property name="SERVICE_NAME" value="my-service"/>
  <property name="LOG_COLLECTOR_URL" value="${LOG_COLLECTOR_URL:-http://localhost:3001/api/logs}"/>
  <property name="LOG_COLLECTOR_ENABLED" value="${LOG_COLLECTOR_ENABLED:-true}"/>

  <!-- HTTP appender (batched - recommended) -->
  <appender name="HTTP" class="com.pcsalt.logcollector.client.HttpLogAppender">
    <url>${LOG_COLLECTOR_URL}</url>
    <serviceName>${SERVICE_NAME}</serviceName>
    <enabled>${LOG_COLLECTOR_ENABLED}</enabled>
    <batchSize>10</batchSize>
    <flushIntervalMs>1000</flushIntervalMs>
  </appender>

  <!-- Use only in local/dev profile -->
  <springProfile name="local">
    <root level="INFO">
      <appender-ref ref="CONSOLE"/>
      <appender-ref ref="HTTP"/>
    </root>
  </springProfile>

  <!-- NO HTTP appender in production -->
  <springProfile name="prod">
    <root level="INFO">
      <appender-ref ref="CONSOLE"/>
    </root>
  </springProfile>
</configuration>
```

### 2. Add Correlation ID (Optional)

Add correlation ID to MDC for distributed tracing:

```kotlin
import org.slf4j.MDC

// In a filter or interceptor
MDC.put("correlationId", UUID.randomUUID().toString())
try {
  // ... handle request
} finally {
  MDC.remove("correlationId")
}
```

## Configuration Options

### HttpLogAppender (Batched)

| Property | Default | Description |
|----------|---------|-------------|
| `url` | `http://localhost:3001/api/logs` | Log collector URL |
| `serviceName` | `unknown` | Service name for identification |
| `enabled` | `true` | Enable/disable appender |
| `batchSize` | `10` | Number of logs per batch |
| `flushIntervalMs` | `1000` | Flush interval in milliseconds |
| `connectTimeoutMs` | `5000` | HTTP connect timeout |
| `requestTimeoutMs` | `10000` | HTTP request timeout |
| `queueCapacity` | `10000` | Max logs in queue |

### AsyncHttpLogAppender (Simple)

| Property | Default | Description |
|----------|---------|-------------|
| `url` | `http://localhost:3001/api/logs` | Log collector URL |
| `serviceName` | `unknown` | Service name for identification |
| `enabled` | `true` | Enable/disable appender |
| `connectTimeoutMs` | `5000` | HTTP connect timeout |
| `requestTimeoutMs` | `10000` | HTTP request timeout |

## Environment Variables

Control appender behavior via environment variables:

```bash
# Disable log collector
LOG_COLLECTOR_ENABLED=false ./gradlew bootRun

# Use different URL
LOG_COLLECTOR_URL=http://log-server:3001/api/logs ./gradlew bootRun
```

## Behavior

- **Async**: Logs are sent asynchronously, never blocking your application
- **Fail-safe**: If log-collector is down, logs are discarded silently
- **Batched**: HttpLogAppender batches logs for efficiency
- **Auto-reconnect**: Automatically resumes when collector becomes available

## Requirements

- Java 11+ (uses java.net.http.HttpClient)
- Logback (included with Spring Boot)
- Jackson (for JSON serialization)
