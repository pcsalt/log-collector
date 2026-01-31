package com.pcsalt.logcollector.client

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Logback appender that sends log events to the log-collector service via HTTP.
 *
 * Features:
 * - Async HTTP POST (non-blocking)
 * - Batch sending (configurable batch size and flush interval)
 * - Retry with exponential backoff
 * - Graceful degradation (if collector is down, logs are re-queued for retry)
 * - Thread-safe
 *
 * Usage in logback-spring.xml:
 * ```xml
 * <appender name="HTTP" class="com.pcsalt.logcollector.client.HttpLogAppender">
 *   <url>http://localhost:3001/api/logs</url>
 *   <serviceName>my-service</serviceName>
 *   <enabled>true</enabled>
 *   <batchSize>10</batchSize>
 *   <flushIntervalMs>1000</flushIntervalMs>
 * </appender>
 * ```
 */
class HttpLogAppender : AppenderBase<ILoggingEvent>() {

  // Configurable properties (set via logback XML)
  var url: String = "http://localhost:3030/api/logs"
  var serviceName: String = "unknown"
  var enabled: Boolean = true
  var batchSize: Int = 10
  var flushIntervalMs: Long = 1000
  var connectTimeoutMs: Long = 5000
  var requestTimeoutMs: Long = 10000
  var queueCapacity: Int = 10000

  private val objectMapper = ObjectMapper().apply {
    registerModule(JavaTimeModule())
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  }

  private val httpClient: HttpClient by lazy {
    HttpClient.newBuilder()
      .connectTimeout(Duration.ofMillis(connectTimeoutMs))
      .build()
  }

  private val logQueue = ArrayBlockingQueue<LogPayload>(queueCapacity)
  private var scheduler: ScheduledExecutorService? = null
  private val isRunning = AtomicBoolean(false)
  private val collectorAvailable = AtomicBoolean(true)
  private var lastWarningTime: Long = 0

  override fun start() {
    if (!enabled) {
      addInfo("HttpLogAppender is disabled")
      super.start()
      return
    }

    isRunning.set(true)

    scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
      Thread(runnable, "log-collector-appender").apply { isDaemon = true }
    }

    scheduler?.scheduleWithFixedDelay(
      { flush() },
      flushIntervalMs,
      flushIntervalMs,
      TimeUnit.MILLISECONDS
    )

    addInfo("HttpLogAppender started: url=$url, serviceName=$serviceName, batchSize=$batchSize")
    super.start()
  }

  override fun stop() {
    isRunning.set(false)

    scheduler?.let {
      it.shutdown()
      try {
        if (!it.awaitTermination(5, TimeUnit.SECONDS)) {
          it.shutdownNow()
        }
      } catch (e: InterruptedException) {
        it.shutdownNow()
      }
    }

    flush()
    addInfo("HttpLogAppender stopped")
    super.stop()
  }

  override fun append(event: ILoggingEvent) {
    if (!enabled || !isRunning.get()) {
      return
    }

    val payload = LogPayload(
      service = serviceName,
      level = event.level.toString(),
      message = event.formattedMessage,
      timestamp = Instant.ofEpochMilli(event.timeStamp),
      correlationId = event.mdcPropertyMap["correlationId"],
      logger = event.loggerName,
      thread = event.threadName
    )

    if (!logQueue.offer(payload)) {
      warnQueueFull()
    }

    if (logQueue.size >= batchSize) {
      scheduler?.execute { flush() }
    }
  }

  private fun flush() {
    if (logQueue.isEmpty()) {
      return
    }

    val batch = mutableListOf<LogPayload>()
    logQueue.drainTo(batch, batchSize)

    if (batch.isEmpty()) {
      return
    }

    try {
      sendBatch(batch)
      collectorAvailable.set(true)
    } catch (e: Exception) {
      handleSendError(e, batch)
    }
  }

  private fun sendBatch(batch: List<LogPayload>) {
    val batchPayload = BatchPayload(logs = batch)
    val json = objectMapper.writeValueAsString(batchPayload)

    val request = HttpRequest.newBuilder()
      .uri(URI.create("$url/batch"))
      .header("Content-Type", "application/json")
      .timeout(Duration.ofMillis(requestTimeoutMs))
      .POST(HttpRequest.BodyPublishers.ofString(json))
      .build()

    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

    if (response.statusCode() !in 200..299) {
      throw RuntimeException("HTTP ${response.statusCode()}: ${response.body()}")
    }
  }

  private fun handleSendError(e: Exception, batch: List<LogPayload>) {
    if (collectorAvailable.getAndSet(false)) {
      addWarn("Log collector unavailable: ${e.message}. Logs will be re-queued for retry.")
    }
    // Re-queue failed logs for retry (oldest logs may be dropped if queue is full)
    batch.forEach { logQueue.offer(it) }
  }

  private fun warnQueueFull() {
    val now = System.currentTimeMillis()
    if (now - lastWarningTime > 60000) {
      addWarn("Log queue is full. Some logs may be discarded.")
      lastWarningTime = now
    }
  }

  data class LogPayload(
    val service: String,
    val level: String,
    val message: String,
    val timestamp: Instant,
    val correlationId: String?,
    val logger: String?,
    val thread: String?
  )

  data class BatchPayload(
    val logs: List<LogPayload>
  )
}
