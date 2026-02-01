/**
 * Log Collector JavaScript Client
 *
 * Features:
 * - Automatic batching with configurable batch size and flush interval
 * - Retry with exponential backoff on failure
 * - Console integration (captures console.log, console.error, etc.)
 * - Global error handler integration
 * - TypeScript support
 */
class LogCollectorClient {
    constructor(config) {
        this.queue = [];
        this.flushTimer = null;
        this.isCollectorAvailable = true;
        this.retryCount = 0;
        this.originalConsole = {};
        this.config = {
            url: config.url,
            serviceName: config.serviceName,
            enabled: config.enabled ?? true,
            batchSize: config.batchSize ?? 10,
            flushIntervalMs: config.flushIntervalMs ?? 1000,
            maxRetries: config.maxRetries ?? 3,
            captureConsole: config.captureConsole ?? false,
            captureErrors: config.captureErrors ?? false,
            correlationIdFn: config.correlationIdFn,
        };
        if (this.config.enabled) {
            this.startFlushTimer();
            if (this.config.captureConsole) {
                this.setupConsoleCapture();
            }
            if (this.config.captureErrors) {
                this.setupErrorCapture();
            }
        }
    }
    startFlushTimer() {
        if (this.flushTimer) {
            clearInterval(this.flushTimer);
        }
        this.flushTimer = setInterval(() => this.flush(), this.config.flushIntervalMs);
    }
    setupConsoleCapture() {
        const levels = [
            { method: 'debug', level: 'DEBUG' },
            { method: 'log', level: 'INFO' },
            { method: 'info', level: 'INFO' },
            { method: 'warn', level: 'WARN' },
            { method: 'error', level: 'ERROR' },
        ];
        levels.forEach(({ method, level }) => {
            const original = console[method];
            this.originalConsole[method] = original;
            console[method] = (...args) => {
                original.apply(console, args);
                const message = args
                    .map((arg) => typeof arg === 'object' ? JSON.stringify(arg) : String(arg))
                    .join(' ');
                this.log(level, message, 'console');
            };
        });
    }
    setupErrorCapture() {
        if (typeof window !== 'undefined') {
            window.addEventListener('error', (event) => {
                this.error(`Uncaught Error: ${event.message} at ${event.filename}:${event.lineno}:${event.colno}`, 'window.onerror');
            });
            window.addEventListener('unhandledrejection', (event) => {
                const reason = event.reason instanceof Error
                    ? `${event.reason.message}\n${event.reason.stack}`
                    : String(event.reason);
                this.error(`Unhandled Promise Rejection: ${reason}`, 'unhandledrejection');
            });
        }
    }
    log(level, message, logger) {
        if (!this.config.enabled)
            return;
        const entry = {
            level,
            message,
            timestamp: new Date().toISOString(),
            correlationId: this.config.correlationIdFn?.(),
            logger,
        };
        this.queue.push(entry);
        if (this.queue.length >= this.config.batchSize) {
            this.flush();
        }
    }
    debug(message, logger) {
        this.log('DEBUG', message, logger);
    }
    info(message, logger) {
        this.log('INFO', message, logger);
    }
    warn(message, logger) {
        this.log('WARN', message, logger);
    }
    error(message, logger) {
        this.log('ERROR', message, logger);
    }
    async flush() {
        if (this.queue.length === 0)
            return;
        const batch = this.queue.splice(0, this.config.batchSize);
        const logs = batch.map((entry) => ({
            ...entry,
            service: this.config.serviceName,
        }));
        try {
            const response = await fetch(`${this.config.url}/batch`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ logs }),
            });
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            this.isCollectorAvailable = true;
            this.retryCount = 0;
        }
        catch (error) {
            this.handleSendError(error, batch);
        }
    }
    handleSendError(error, batch) {
        if (this.isCollectorAvailable) {
            console.warn(`[LogCollector] Collector unavailable: ${error instanceof Error ? error.message : error}. Logs will be re-queued.`);
            this.isCollectorAvailable = false;
        }
        // Re-queue logs for retry
        if (this.retryCount < this.config.maxRetries) {
            this.queue.unshift(...batch);
            this.retryCount++;
        }
        else {
            console.warn(`[LogCollector] Max retries reached. Dropping ${batch.length} logs.`);
            this.retryCount = 0;
        }
    }
    async shutdown() {
        if (this.flushTimer) {
            clearInterval(this.flushTimer);
            this.flushTimer = null;
        }
        // Restore original console methods
        Object.entries(this.originalConsole).forEach(([method, fn]) => {
            if (fn) {
                console[method] = fn;
            }
        });
        // Final flush
        while (this.queue.length > 0) {
            await this.flush();
        }
    }
    getQueueSize() {
        return this.queue.length;
    }
    isAvailable() {
        return this.isCollectorAvailable;
    }
}
// Singleton instance
let instance = null;
export function initLogCollector(config) {
    if (instance) {
        console.warn('[LogCollector] Already initialized. Returning existing instance.');
        return instance;
    }
    instance = new LogCollectorClient(config);
    return instance;
}
export function getLogCollector() {
    return instance;
}
export { LogCollectorClient };
export default LogCollectorClient;
//# sourceMappingURL=index.js.map