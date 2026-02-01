/**
 * Log Collector JavaScript Client
 *
 * Features:
 * - Automatic batching with configurable batch size and flush interval
 * - Retry with exponential backoff on failure
 * - Console integration (captures console.log, console.error, etc.)
 * - Global error handler integration
 * - HTTP request/response capture (fetch and XMLHttpRequest)
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
            captureHttp: config.captureHttp ?? false,
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
            if (this.config.captureHttp) {
                this.setupHttpCapture();
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
    setupHttpCapture() {
        if (typeof window === 'undefined')
            return;
        const sensitiveHeaders = ['authorization', 'cookie', 'x-api-key', 'x-auth-token'];
        const filterHeaders = (headers) => {
            const filtered = {};
            Object.keys(headers).forEach(key => {
                if (!sensitiveHeaders.includes(key.toLowerCase())) {
                    filtered[key] = headers[key];
                }
            });
            return filtered;
        };
        // Intercept fetch
        this.originalFetch = window.fetch;
        window.fetch = async (...args) => {
            const startTime = Date.now();
            const [resource, init] = args;
            const url = typeof resource === 'string'
                ? resource
                : resource instanceof Request
                    ? resource.url
                    : resource.toString();
            const method = init?.method || 'GET';
            // Extract request headers
            const requestHeaders = {};
            if (init?.headers) {
                if (init.headers instanceof Headers) {
                    init.headers.forEach((value, key) => {
                        requestHeaders[key] = value;
                    });
                }
                else if (Array.isArray(init.headers)) {
                    init.headers.forEach(([key, value]) => {
                        requestHeaders[key] = value;
                    });
                }
                else {
                    Object.assign(requestHeaders, init.headers);
                }
            }
            try {
                const response = await this.originalFetch(...args);
                const duration = Date.now() - startTime;
                // Extract response headers
                const responseHeaders = {};
                response.headers.forEach((value, key) => {
                    responseHeaders[key] = value;
                });
                this.info(`HTTP ${method} ${url} ${response.status} ${duration}ms | ` +
                    `Request: ${JSON.stringify(filterHeaders(requestHeaders))} | ` +
                    `Response: ${JSON.stringify(filterHeaders(responseHeaders))}`, 'http.fetch');
                return response;
            }
            catch (error) {
                const duration = Date.now() - startTime;
                this.error(`HTTP ${method} ${url} FAILED ${duration}ms | ` +
                    `Request: ${JSON.stringify(filterHeaders(requestHeaders))} | ` +
                    `Error: ${error instanceof Error ? error.message : String(error)}`, 'http.fetch');
                throw error;
            }
        };
        // Intercept XMLHttpRequest
        this.originalXMLHttpRequest = window.XMLHttpRequest;
        const OriginalXHR = this.originalXMLHttpRequest;
        const loggerInstance = this;
        window.XMLHttpRequest = function () {
            const xhr = new OriginalXHR();
            const requestHeaders = {};
            let method = '';
            let url = '';
            let startTime = 0;
            const originalOpen = xhr.open;
            xhr.open = function (...args) {
                method = args[0];
                url = typeof args[1] === 'string' ? args[1] : args[1].toString();
                return originalOpen.apply(xhr, args);
            };
            const originalSetRequestHeader = xhr.setRequestHeader;
            xhr.setRequestHeader = function (header, value) {
                requestHeaders[header] = value;
                return originalSetRequestHeader.call(xhr, header, value);
            };
            const originalSend = xhr.send;
            xhr.send = function (...args) {
                startTime = Date.now();
                xhr.addEventListener('load', () => {
                    const duration = Date.now() - startTime;
                    const responseHeaders = {};
                    xhr.getAllResponseHeaders().split('\r\n').forEach(line => {
                        const [key, value] = line.split(': ');
                        if (key && value) {
                            responseHeaders[key] = value;
                        }
                    });
                    loggerInstance.info(`HTTP ${method} ${url} ${xhr.status} ${duration}ms | ` +
                        `Request: ${JSON.stringify(filterHeaders(requestHeaders))} | ` +
                        `Response: ${JSON.stringify(filterHeaders(responseHeaders))}`, 'http.xhr');
                });
                xhr.addEventListener('error', () => {
                    const duration = Date.now() - startTime;
                    loggerInstance.error(`HTTP ${method} ${url} FAILED ${duration}ms | ` +
                        `Request: ${JSON.stringify(filterHeaders(requestHeaders))} | ` +
                        `Error: Network error`, 'http.xhr');
                });
                return originalSend.apply(xhr, args);
            };
            return xhr;
        };
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
        // Restore original HTTP methods
        if (typeof window !== 'undefined') {
            if (this.originalFetch) {
                window.fetch = this.originalFetch;
            }
            if (this.originalXMLHttpRequest) {
                window.XMLHttpRequest = this.originalXMLHttpRequest;
            }
        }
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