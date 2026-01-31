# Log Collector TypeScript Client

A TypeScript client for sending logs to the log-collector service.

## Features

- Full TypeScript support with type definitions
- Automatic batching with configurable batch size and flush interval
- Retry with re-queuing on failure
- Console integration (captures console.log, console.error, etc.)
- Global error handler integration (window.onerror, unhandledrejection)
- Works in browser and Node.js

## Installation

```bash
# From local path
npm install /path/to/log-collector/clients/js

# Or copy the src/index.ts file to your project
```

## Build

```bash
cd clients/js
npm install
npm run build
```

## Usage

### Basic Usage

```typescript
import { LogCollectorClient } from '@pcsalt/log-collector-client';

const logger = new LogCollectorClient({
  url: 'http://localhost:3030/api/logs',
  serviceName: 'my-frontend'
});

logger.info('User logged in');
logger.warn('Session expiring soon');
logger.error('Failed to fetch data');
logger.debug('Button clicked');
```

### With Console Capture

Automatically captures all console.log, console.error, etc. calls:

```typescript
import { LogCollectorClient } from '@pcsalt/log-collector-client';

const logger = new LogCollectorClient({
  url: 'http://localhost:3030/api/logs',
  serviceName: 'my-frontend',
  captureConsole: true
});

// These will be sent to log-collector automatically
console.log('This is logged');
console.error('This error is captured');
```

### With Error Capture

Automatically captures uncaught errors and unhandled promise rejections:

```typescript
import { LogCollectorClient } from '@pcsalt/log-collector-client';

const logger = new LogCollectorClient({
  url: 'http://localhost:3030/api/logs',
  serviceName: 'my-frontend',
  captureErrors: true
});

// These will be captured and sent
throw new Error('Uncaught error');
Promise.reject('Unhandled rejection');
```

### Full Configuration

```typescript
import { LogCollectorClient, LogCollectorConfig } from '@pcsalt/log-collector-client';

const config: LogCollectorConfig = {
  url: 'http://localhost:3030/api/logs',
  serviceName: 'my-frontend',
  enabled: true,                    // Enable/disable logging (default: true)
  batchSize: 10,                    // Logs per batch (default: 10)
  flushIntervalMs: 1000,            // Flush interval in ms (default: 1000)
  maxRetries: 3,                    // Max retry attempts (default: 3)
  captureConsole: true,             // Capture console.* calls (default: false)
  captureErrors: true,              // Capture global errors (default: false)
  correlationIdFn: () => {          // Optional: provide correlation ID
    return sessionStorage.getItem('correlationId') || undefined;
  }
};

const logger = new LogCollectorClient(config);
```

### React Integration

```typescript
// src/logger.ts
import { LogCollectorClient } from '@pcsalt/log-collector-client';

export const logger = new LogCollectorClient({
  url: import.meta.env.VITE_LOG_COLLECTOR_URL || 'http://localhost:3030/api/logs',
  serviceName: 'my-frontend',
  enabled: import.meta.env.DEV,
  captureConsole: true,
  captureErrors: true
});

// src/ErrorBoundary.tsx
import React, { Component, ReactNode } from 'react';
import { logger } from './logger';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo): void {
    logger.error(
      `React Error: ${error.message}\n${errorInfo.componentStack}`,
      'ErrorBoundary'
    );
  }

  render(): ReactNode {
    if (this.state.hasError) {
      return <h1>Something went wrong.</h1>;
    }
    return this.props.children;
  }
}

export default ErrorBoundary;
```

### Cleanup

```typescript
// On page unload or app shutdown
window.addEventListener('beforeunload', () => {
  logger.shutdown();
});
```

## API

### Types

```typescript
type LogLevel = 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';

interface LogEntry {
  service: string;
  level: LogLevel;
  message: string;
  timestamp: string;
  correlationId?: string;
  logger?: string;
  thread?: string;
}

interface LogCollectorConfig {
  url: string;
  serviceName: string;
  enabled?: boolean;
  batchSize?: number;
  flushIntervalMs?: number;
  maxRetries?: number;
  captureConsole?: boolean;
  captureErrors?: boolean;
  correlationIdFn?: () => string | undefined;
}
```

### LogCollectorClient Methods

| Method | Description |
|--------|-------------|
| `debug(message: string, logger?: string)` | Log debug message |
| `info(message: string, logger?: string)` | Log info message |
| `warn(message: string, logger?: string)` | Log warning message |
| `error(message: string, logger?: string)` | Log error message |
| `flush(): Promise<void>` | Manually flush the log queue |
| `shutdown(): Promise<void>` | Stop the client and flush remaining logs |
| `getQueueSize(): number` | Get number of queued logs |
| `isAvailable(): boolean` | Check if collector is available |

### Helper Functions

| Function | Description |
|----------|-------------|
| `initLogCollector(config)` | Initialize singleton instance |
| `getLogCollector()` | Get singleton instance |

## Environment-based Configuration

Only enable in development:

```typescript
const logger = new LogCollectorClient({
  url: 'http://localhost:3030/api/logs',
  serviceName: 'my-frontend',
  enabled: import.meta.env.DEV  // Vite
  // or: process.env.NODE_ENV === 'development'  // CRA/Node
});
```
