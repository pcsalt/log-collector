# Log Collector

A lightweight, centralized logging service for collecting, storing, and visualizing logs from distributed applications.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![GitHub release](https://img.shields.io/github/v/release/pcsalt/log-collector)](https://github.com/pcsalt/log-collector/releases)
[![Status](https://img.shields.io/badge/Status-Active%20Development-green.svg)](https://github.com/pcsalt/log-collector)

> **Status:** Active Development - v1.0.0 release coming soon! Currently in Phase 1 (Configuration & Docker support complete). See [RELEASE_PLAN.md](RELEASE_PLAN.md) for roadmap.

## Table of Contents

- [Features](#features)
- [Screenshots](#screenshots)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Client Libraries](#client-libraries)
- [API Endpoints](#api-endpoints)
- [Docker Deployment](#docker-deployment)
- [Use Cases](#use-cases)
- [Architecture](#architecture)
- [Performance](#performance)
- [Development](#development)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

## Features

- **🚀 Lightweight** - Single JAR deployment, minimal resource usage
- **📊 Real-time Dashboard** - Live log streaming with WebSocket
- **🔍 Powerful Filtering** - Filter by service, level, correlation ID, time range
- **📦 Automatic Batching** - Efficient log ingestion with configurable batch sizes
- **🔄 Retry Logic** - Automatic retry with exponential backoff
- **🧹 Auto Cleanup** - Configurable log retention policies
- **🐳 Docker Ready** - Docker and docker-compose support
- **📚 Multiple Clients** - JavaScript and Kotlin client libraries
- **🎯 HTTP Capture** - Automatic request/response logging (JavaScript client)
- **💾 SQLite Storage** - Simple, file-based database (no external dependencies)

## Screenshots

### Dashboard
![Dashboard](docs/images/dashboard.png)
*Real-time log streaming with filtering and search capabilities*

### Log Details
![Log Details](docs/images/log-details.png)
*Detailed view with correlation ID, logger, thread information*

> **Note:** Screenshots coming soon. Access the dashboard at http://localhost:7777 after starting the service.

## Quick Start

### Using Docker (Recommended)

```bash
# Clone the repository
git clone https://github.com/pcsalt/log-collector.git
cd log-collector

# Start with docker-compose
docker-compose up -d

# Access dashboard
open http://localhost:7777
```

### Using Pre-built JAR

```bash
# Download latest release
wget https://github.com/pcsalt/log-collector/releases/latest/download/log-collector.jar

# Run the service
java -jar log-collector.jar

# Access dashboard
open http://localhost:7777
```

### Building from Source

```bash
# Clone the repository
git clone https://github.com/pcsalt/log-collector.git
cd log-collector

# Build
./gradlew build

# Run
java -jar build/libs/log-collector.jar
```

## Configuration

Configure the service using environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 7777 | Port on which the service runs |
| `DATABASE_PATH` | ./data/logs.db | Path to SQLite database |
| `LOG_RETENTION_HOURS` | 24 | How long to keep logs (hours) |
| `LOG_RETENTION_CRON` | 0 0 * * * * | Cleanup schedule (cron expression) |
| `CLEANUP_ENABLED` | true | Enable automatic log cleanup |

### Example `.env` file

```bash
SERVER_PORT=7777
DATABASE_PATH=/app/data/logs.db
LOG_RETENTION_HOURS=168  # 7 days
LOG_RETENTION_CRON=0 0 2 * * *  # Daily at 2 AM
CLEANUP_ENABLED=true
```

## Client Libraries

### JavaScript/TypeScript Client

Perfect for frontend applications (React, Vue, Angular, etc.)

**Installation:**

For now, install from local path (npm publishing coming soon):
```bash
npm install /path/to/log-collector/clients/js
```

After v1.0.0 release, install from npm:
```bash
npm install @pcsalt/log-collector-client
```

**Usage:**
```typescript
import { LogCollectorClient } from '@pcsalt/log-collector-client';

const logger = new LogCollectorClient({
  url: 'http://localhost:7777/api/logs',
  serviceName: 'my-frontend',
  captureConsole: true,  // Auto-capture console.log, etc.
  captureErrors: true,   // Auto-capture uncaught errors
  captureHttp: true,     // Auto-capture HTTP requests/responses
});

// Manual logging
logger.info('User logged in');
logger.error('Failed to fetch data');
```

[**Full JavaScript Client Documentation →**](clients/js/README.md)

### Kotlin Client

For Spring Boot and Kotlin backend services.

**Installation:**

For now, install from local Maven repository:
```bash
# Publish to local Maven
cd /path/to/log-collector
./gradlew publishToMavenLocal
```

```kotlin
// build.gradle.kts
repositories {
    mavenLocal()  // Add this
    mavenCentral()
}

dependencies {
    implementation("com.pcsalt:log-collector-client:1.0.0-SNAPSHOT")
}
```

After v1.0.0 release, install from Maven Central:
```kotlin
dependencies {
    implementation("com.pcsalt:log-collector-client:1.0.0")
}
```

**Usage:**
```kotlin
// logback-spring.xml
<appender name="HTTP" class="com.pcsalt.logcollector.client.AsyncHttpLogAppender">
  <url>http://localhost:7777/api/logs</url>
  <serviceName>my-backend-service</serviceName>
</appender>
```

[**Full Kotlin Client Documentation →**](clients/kotlin/README.md)

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Dashboard UI |
| `/api/logs` | GET | Query logs |
| `/api/logs` | POST | Send single log |
| `/api/logs/batch` | POST | Send batch of logs |
| `/api/logs/services` | GET | Get distinct services |
| `/api/logs/stats` | GET | Get statistics |
| `/ws` | WebSocket | Live log streaming |
| `/actuator/health` | GET | Health check |

### Example: Send Logs

```bash
# Single log
curl -X POST http://localhost:7777/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "service": "my-app",
    "level": "INFO",
    "message": "Application started",
    "timestamp": "2026-02-01T12:00:00Z"
  }'

# Batch logs
curl -X POST http://localhost:7777/api/logs/batch \
  -H "Content-Type: application/json" \
  -d '{
    "logs": [
      {
        "service": "my-app",
        "level": "INFO",
        "message": "User logged in",
        "correlationId": "abc-123"
      }
    ]
  }'
```

## Docker Deployment

### Basic Deployment

**Using the repository:**
```bash
git clone https://github.com/pcsalt/log-collector.git
cd log-collector
docker-compose up -d
```

**Using pre-built image (after v1.0.0 release):**
```yaml
version: '3.8'
services:
  log-collector:
    image: krrishnaaaa/log-collector:latest
    ports:
      - "7777:7777"
    environment:
      - LOG_RETENTION_HOURS=168  # 7 days
    volumes:
      - log-data:/app/data
    restart: unless-stopped

volumes:
  log-data:
```

For detailed Docker deployment options, see [Docker Deployment Guide](docs/DOCKER.md).

### With Custom Configuration

```bash
# Create .env file
cp .env.example .env

# Edit configuration
nano .env

# Start services
docker-compose up -d
```

## Use Cases

- **Microservices Logging** - Centralize logs from multiple services
- **Frontend Monitoring** - Track frontend errors and user actions
- **Debugging** - Correlation ID tracking across services
- **Audit Trails** - Keep track of important events
- **Development** - Local logging during development

## Architecture

```
┌─────────────────┐
│   Frontend App  │──┐
└─────────────────┘  │
                     │
┌─────────────────┐  │    ┌──────────────────┐
│  Backend API 1  │──┼───▶│  Log Collector   │
└─────────────────┘  │    │                  │
                     │    │  - REST API      │
┌─────────────────┐  │    │  - WebSocket     │
│  Backend API 2  │──┘    │  - Dashboard     │
└─────────────────┘       │  - SQLite DB     │
                          └──────────────────┘
```

## Performance

- **Throughput:** ~10,000 logs/second
- **Storage:** ~1MB per 10,000 logs (compressed)
- **Memory:** ~256MB base + ~1MB per 10,000 queued logs
- **Database:** SQLite with automatic cleanup

## Development

```bash
# Clone repository
git clone https://github.com/pcsalt/log-collector.git
cd log-collector

# Build
./gradlew build

# Run tests
./gradlew test

# Run locally
./gradlew bootRun

# Build Docker image
docker build -t log-collector .
```

## Contributing

Contributions are welcome! Whether you want to:
- 🐛 Report a bug
- 💡 Suggest a new feature
- 📝 Improve documentation
- 🔧 Submit a pull request

Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines (coming soon).

For now, feel free to [open an issue](https://github.com/pcsalt/log-collector/issues) to discuss your ideas!

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Issues:** [GitHub Issues](https://github.com/pcsalt/log-collector/issues)
- **Discussions:** [GitHub Discussions](https://github.com/pcsalt/log-collector/discussions)
- **Documentation:** [docs/](docs/)

## Roadmap

- [ ] Authentication & API keys
- [ ] Rate limiting
- [ ] Elasticsearch integration
- [ ] Prometheus metrics
- [ ] Additional client SDKs (Python, Go, Ruby)
- [ ] Kubernetes Helm charts
- [ ] Log export (CSV, JSON)

---

Made with ❤️ by [pcsalt](https://github.com/pcsalt)
