# Contributing to Log Collector

First off, thank you for considering contributing to Log Collector! It's people like you that make this project better for everyone.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Testing Guidelines](#testing-guidelines)

## Code of Conduct

This project and everyone participating in it is governed by our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates. When creating a bug report, include:

- **Use a clear and descriptive title**
- **Describe the exact steps to reproduce the problem**
- **Provide specific examples** (code snippets, configurations, etc.)
- **Describe the behavior you observed** and what you expected
- **Include logs and error messages**
- **Specify your environment:**
  - OS and version
  - Java/Kotlin version
  - Docker version (if applicable)
  - Browser (for frontend issues)

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion:

- **Use a clear and descriptive title**
- **Provide a detailed description** of the proposed functionality
- **Explain why this enhancement would be useful**
- **List any alternative solutions** you've considered

### Your First Code Contribution

Unsure where to start? Look for issues labeled:

- `good first issue` - Simple issues suitable for beginners
- `help wanted` - Issues where we need community help
- `documentation` - Documentation improvements

### Pull Requests

1. Fork the repository and create your branch from `main`
2. Make your changes
3. Add tests for your changes
4. Ensure all tests pass
5. Update documentation as needed
6. Submit a pull request

## Development Setup

### Prerequisites

- Java 21 or higher
- Kotlin 2.0.21+
- Docker (for containerized development)
- Git

### Clone and Build

```bash
# Clone repository
git clone https://github.com/pcsalt/log-collector.git
cd log-collector

# Build project
./gradlew build

# Run tests
./gradlew test

# Run locally
./gradlew bootRun
```

### Docker Development

```bash
# Build Docker image
docker build -t log-collector:dev .

# Run with docker-compose
docker-compose up -d

# View logs
docker-compose logs -f
```

### Client Development

#### JavaScript Client

```bash
cd clients/js

# Install dependencies
npm install

# Build
npm run build

# Run tests (when available)
npm test
```

#### Kotlin Client

```bash
# Build and publish to local Maven
./gradlew publishToMavenLocal
```

## Pull Request Process

1. **Create a branch** with a descriptive name:
   - Feature: `feature/add-authentication`
   - Bug fix: `fix/correlation-id-filter`
   - Documentation: `docs/update-readme`

2. **Write clear commit messages** (see guidelines below)

3. **Add/update tests** for your changes

4. **Update documentation:**
   - README.md for user-facing changes
   - Code comments for complex logic
   - API documentation if endpoints change

5. **Ensure CI passes:**
   - All tests pass
   - Code builds successfully
   - No linting errors

6. **Update CHANGELOG.md** under `[Unreleased]` section

7. **Submit PR with:**
   - Clear title describing the change
   - Description of what changed and why
   - Link to related issues
   - Screenshots (for UI changes)

8. **Address review feedback** promptly

## Coding Standards

### Kotlin

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use 2 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful variable and function names
- Add KDoc comments for public APIs

```kotlin
/**
 * Processes log entries and stores them in the database.
 *
 * @param logs List of log entries to process
 * @return Number of logs successfully processed
 */
fun processLogs(logs: List<LogEntry>): Int {
  // Implementation
}
```

### TypeScript/JavaScript

- Follow [TypeScript Style Guide](https://google.github.io/styleguide/tsguide.html)
- Use 2 spaces for indentation
- Use meaningful variable and function names
- Add JSDoc comments for exported functions

```typescript
/**
 * Initializes the log collector client
 *
 * @param config - Configuration options
 * @returns Initialized logger instance
 */
export function initLogCollector(config: LogCollectorConfig): LogCollectorClient {
  // Implementation
}
```

### General Guidelines

- **No wildcard imports** - Always use specific imports
- **Keep functions small** - Single responsibility principle
- **Write self-documenting code** - Clear names over comments
- **Handle errors properly** - Don't swallow exceptions
- **Avoid magic numbers** - Use named constants
- **Be consistent** - Follow existing code style

## Commit Message Guidelines

We follow [Conventional Commits](https://www.conventionalcommits.org/) specification.

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding/updating tests
- `chore`: Maintenance tasks
- `perf`: Performance improvements
- `ci`: CI/CD changes

### Examples

```
feat(client): add retry logic to HTTP client

Implement exponential backoff retry mechanism for failed
HTTP requests. Retries up to 3 times with configurable delays.

Closes #42
```

```
fix(dashboard): correlation ID filter not working

Update loadInitialLogs() to send correlationId parameter
to backend API instead of client-side filtering only.

Fixes #58
```

```
docs: update Docker deployment guide

Add examples for Kubernetes deployment and
environment variable configuration.
```

### Rules

- Use present tense: "add feature" not "added feature"
- Use imperative mood: "move cursor" not "moves cursor"
- Keep subject line under 72 characters
- Reference issues and PRs in footer

## Testing Guidelines

### Unit Tests

- Write tests for all new features
- Aim for >70% code coverage
- Test edge cases and error conditions
- Use descriptive test names

```kotlin
@Test
fun `should filter logs by correlation ID`() {
  // Arrange
  val correlationId = "test-123"

  // Act
  val result = logService.filterByCorrelationId(correlationId)

  // Assert
  assertThat(result).allMatch { it.correlationId == correlationId }
}
```

### Integration Tests

- Test complete workflows
- Use test containers when needed
- Clean up test data after tests

### Running Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests LogServiceTest

# With coverage
./gradlew test jacocoTestReport
```

## Questions?

Feel free to:
- Open an issue for questions
- Start a [discussion](https://github.com/pcsalt/log-collector/discussions)
- Reach out to maintainers

## Recognition

Contributors will be recognized in:
- CONTRIBUTORS.md file
- GitHub contributors page
- Release notes

Thank you for contributing! 🎉
