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
export type LogLevel = 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';
export interface LogEntry {
    service: string;
    level: LogLevel;
    message: string;
    timestamp: string;
    correlationId?: string;
    logger?: string;
    thread?: string;
}
export interface LogCollectorConfig {
    url: string;
    serviceName: string;
    enabled?: boolean;
    batchSize?: number;
    flushIntervalMs?: number;
    maxRetries?: number;
    captureConsole?: boolean;
    captureErrors?: boolean;
    captureHttp?: boolean;
    correlationIdFn?: () => string | undefined;
}
declare class LogCollectorClient {
    private config;
    private queue;
    private flushTimer;
    private isCollectorAvailable;
    private retryCount;
    private originalConsole;
    private originalFetch?;
    private originalXMLHttpRequest?;
    constructor(config: LogCollectorConfig);
    private startFlushTimer;
    private setupConsoleCapture;
    private setupErrorCapture;
    private setupHttpCapture;
    private log;
    debug(message: string, logger?: string): void;
    info(message: string, logger?: string): void;
    warn(message: string, logger?: string): void;
    error(message: string, logger?: string): void;
    flush(): Promise<void>;
    private handleSendError;
    shutdown(): Promise<void>;
    getQueueSize(): number;
    isAvailable(): boolean;
}
export declare function initLogCollector(config: LogCollectorConfig): LogCollectorClient;
export declare function getLogCollector(): LogCollectorClient | null;
export { LogCollectorClient };
export default LogCollectorClient;
//# sourceMappingURL=index.d.ts.map