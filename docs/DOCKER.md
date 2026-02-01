# Docker Deployment Guide

This guide covers different ways to deploy Log Collector using Docker.

## Quick Start

### Using Docker Compose (Recommended)

```bash
# Clone repository
git clone https://github.com/pcsalt/log-collector.git
cd log-collector

# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

Access the dashboard at: http://localhost:7777

## Using Docker CLI

### Pull from Registry (Coming Soon)

```bash
docker pull ghcr.io/pcsalt/log-collector:latest
```

### Build Locally

```bash
# Build image
docker build -t log-collector:latest .

# Run container
docker run -d \
  --name log-collector \
  -p 7777:7777 \
  -v log-data:/app/data \
  -e LOG_RETENTION_HOURS=168 \
  log-collector:latest

# View logs
docker logs -f log-collector

# Stop container
docker stop log-collector
docker rm log-collector
```

## Configuration

### Environment Variables

Pass environment variables using `-e` flag:

```bash
docker run -d \
  -p 7777:7777 \
  -e SERVER_PORT=7777 \
  -e DATABASE_PATH=/app/data/logs.db \
  -e LOG_RETENTION_HOURS=168 \
  -e CLEANUP_ENABLED=true \
  log-collector:latest
```

### Using .env File

```bash
# Create .env file
cat > .env <<EOF
SERVER_PORT=7777
LOG_RETENTION_HOURS=168
DATABASE_PATH=/app/data/logs.db
EOF

# Run with env file
docker run -d \
  -p 7777:7777 \
  --env-file .env \
  -v log-data:/app/data \
  log-collector:latest
```

## Volumes

### Persist Database

Always mount a volume to persist logs across container restarts:

```bash
# Named volume (recommended)
docker run -d \
  -v log-data:/app/data \
  log-collector:latest

# Bind mount
docker run -d \
  -v /path/on/host/data:/app/data \
  log-collector:latest
```

## Health Checks

The Docker image includes built-in health checks:

```bash
# Check container health
docker inspect --format='{{.State.Health.Status}}' log-collector

# View health check logs
docker inspect --format='{{json .State.Health}}' log-collector | jq
```

## Docker Compose Examples

### Basic Setup

```yaml
version: '3.8'
services:
  log-collector:
    image: log-collector:latest
    ports:
      - "7777:7777"
    volumes:
      - log-data:/app/data
    restart: unless-stopped

volumes:
  log-data:
```

### Production Setup

```yaml
version: '3.8'
services:
  log-collector:
    image: log-collector:latest
    container_name: log-collector
    ports:
      - "7777:7777"
    environment:
      - SERVER_PORT=7777
      - LOG_RETENTION_HOURS=720  # 30 days
      - LOG_RETENTION_CRON=0 0 2 * * *  # Daily at 2 AM
      - CLEANUP_ENABLED=true
      - JAVA_OPTS=-Xms512m -Xmx1024m
    volumes:
      - ./data:/app/data
      - ./logs:/app/logs
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:7777/actuator/health"]
      interval: 30s
      timeout: 3s
      retries: 3
    networks:
      - app-network

volumes:
  log-data:

networks:
  app-network:
    driver: bridge
```

### With Application Services

```yaml
version: '3.8'
services:
  log-collector:
    image: log-collector:latest
    ports:
      - "7777:7777"
    volumes:
      - log-data:/app/data
    networks:
      - app-network

  backend-api:
    image: my-backend:latest
    environment:
      - LOG_COLLECTOR_URL=http://log-collector:7777/api/logs
    depends_on:
      - log-collector
    networks:
      - app-network

  frontend:
    image: my-frontend:latest
    environment:
      - VITE_LOG_COLLECTOR_URL=http://log-collector:7777/api/logs
    depends_on:
      - log-collector
    networks:
      - app-network

volumes:
  log-data:

networks:
  app-network:
    driver: bridge
```

## Networking

### Internal Network

For services within the same Docker network:

```bash
# Log collector URL
http://log-collector:7777/api/logs
```

### External Access

For external services or host machine:

```bash
# Log collector URL
http://localhost:7777/api/logs
```

## Resource Limits

Limit CPU and memory usage:

```yaml
services:
  log-collector:
    image: log-collector:latest
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

## Logging

Configure Docker logging driver:

```yaml
services:
  log-collector:
    image: log-collector:latest
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

## Troubleshooting

### Check Container Status

```bash
docker ps -a | grep log-collector
```

### View Container Logs

```bash
docker logs log-collector
docker logs -f log-collector  # Follow
docker logs --tail 100 log-collector  # Last 100 lines
```

### Inspect Container

```bash
docker inspect log-collector
```

### Access Container Shell

```bash
docker exec -it log-collector sh
```

### Check Database

```bash
# Access container
docker exec -it log-collector sh

# Check database file
ls -lh /app/data/logs.db

# Check disk usage
du -sh /app/data
```

### Restart Container

```bash
docker restart log-collector
```

### Clean Restart

```bash
docker-compose down
docker-compose up -d
```

## Backup & Restore

### Backup Database

```bash
# Stop container
docker-compose down

# Backup data volume
docker run --rm \
  -v log-data:/data \
  -v $(pwd)/backup:/backup \
  alpine tar czf /backup/log-data-backup-$(date +%Y%m%d).tar.gz /data

# Start container
docker-compose up -d
```

### Restore Database

```bash
# Stop container
docker-compose down

# Restore data volume
docker run --rm \
  -v log-data:/data \
  -v $(pwd)/backup:/backup \
  alpine sh -c "cd /data && tar xzf /backup/log-data-backup-YYYYMMDD.tar.gz --strip 1"

# Start container
docker-compose up -d
```

## Security

### Run as Non-Root User

The Docker image runs as a non-root user by default (appuser).

### Read-Only Root Filesystem

```yaml
services:
  log-collector:
    image: log-collector:latest
    read_only: true
    tmpfs:
      - /tmp
    volumes:
      - log-data:/app/data
```

### Drop Capabilities

```yaml
services:
  log-collector:
    image: log-collector:latest
    cap_drop:
      - ALL
    cap_add:
      - NET_BIND_SERVICE
```

## Performance Tuning

### JVM Settings

```yaml
services:
  log-collector:
    environment:
      - JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### Database Tuning

Mount database on fast storage (SSD):

```yaml
volumes:
  log-data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /fast-ssd/log-collector-data
```

## Monitoring

### View Metrics

```bash
# CPU and Memory usage
docker stats log-collector

# Health status
curl http://localhost:7777/actuator/health
```

### Integration with Monitoring Tools

Use Docker labels for integration with Prometheus, Grafana, etc.:

```yaml
services:
  log-collector:
    labels:
      - "prometheus.io/scrape=true"
      - "prometheus.io/port=7777"
      - "prometheus.io/path=/actuator/prometheus"
```

## Next Steps

- [Configuration Guide](CONFIGURATION.md)
- [API Documentation](API.md)
- [Client Libraries](CLIENTS.md)
- [Kubernetes Deployment](KUBERNETES.md)
