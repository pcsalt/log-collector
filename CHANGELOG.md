# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **API Key Authentication** - Protect `/api/**` endpoints with `X-API-Key` header
  - Set `API_KEY` env var to enable authentication
  - Use `API_KEY=auto` to auto-generate a secure key on startup
  - Dashboard and health endpoints remain public
- Environment variable configuration for all major settings
- Docker support with multi-stage build
- docker-compose.yml for easy deployment
- Comprehensive documentation (README, Docker guide)
- MIT License for open source use
- HTTP request/response capture in JavaScript client
- Correlation ID filtering in dashboard
- Real-time log streaming via WebSocket

### Changed
- Service port configurable via `SERVER_PORT` (default: 7777)
- Database path configurable via `DATABASE_PATH`
- Log retention configurable via `LOG_RETENTION_HOURS` and `LOG_RETENTION_CRON`

### Security
- API Key authentication for API endpoints
- Non-root user in Docker container
- Sensitive header filtering in HTTP capture
- Input validation for log entries
- Google Distroless base image (minimal attack surface)

## [0.1.0] - 2026-02-01

### Added
- Initial release
- Spring Boot backend service
- SQLite database for log storage
- Real-time dashboard with WebSocket support
- REST API for log ingestion and querying
- JavaScript/TypeScript client library
- Kotlin client library for backend services
- Automatic log batching and retry logic
- Configurable log retention and cleanup
- Filter logs by service, level, correlation ID, time range
- Console and error capture in JavaScript client

[Unreleased]: https://github.com/pcsalt/log-collector/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/pcsalt/log-collector/releases/tag/v0.1.0
