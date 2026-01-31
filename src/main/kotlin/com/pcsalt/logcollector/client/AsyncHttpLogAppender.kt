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
import java.util.concurrent.CompletableFuture

/**
 * Simple async HTTP appender that sends each log immediately.
 * Use this for low-volume logging or when immediate delivery is preferred.
 *
 * For high-volume logging, use HttpLogAppender with batching instead.
 *
 * Usage in logback-spring.xml:
 * ```xml
 * <appender name="HTTP_ASYNC" class="com.pcsalt.logcollector.client.AsyncHttpLogAppender">
 *   <url>http://localhost:3001/api/logs</url>
 *   <serviceName>my-service</serviceName>
 *   <enabled>true</enabled>
 * </appender>
 * ```
 */
class AsyncHttpLogAppender : AppenderBase<ILoggingEvent>() {

  var url: String = "http://localhost:3030/api/logs"
  var serviceName: String = "unknown"
  var enabled: Boolean = true
  var connectTimeoutMs: Long = 5000
  var requestTimeoutMs: Long = 10000

  private val objectMapper = ObjectMapper().apply {
    registerModule(JavaTimeModule())
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  }

  private val httpClient: HttpClient by lazy {
    HttpClient.newBuilder()
      .connectTimeout(Duration.ofMillis(connectTimeoutMs))
      .build()
  }

  private var collectorAvailable = true
  private var lastWarningTime: Long = 0

  override fun start() {
    if (!enabled) {
      addInfo("AsyncHttpLogAppender is disabled")
    } else {
      addInfo("AsyncHttpLogAppender started: url=$url, serviceName=$serviceName")
    }
    super.start()
  }

  override fun append(event: ILoggingEvent) {
    if (!enabled) {
      return
    }

    val payload = mapOf(
      "service" to serviceName,
      "level" to event.level.toString(),
      "message" to event.formattedMessage,
      "timestamp" to Instant.ofEpochMilli(event.timeStamp).toString(),
      "correlationId" to event.mdcPropertyMap["correlationId"],
      "logger" to event.loggerName,
      "thread" to event.threadName
    )

    sendAsync(payload)
  }

  private fun sendAsync(payload: Map<String, String?>) {
    try {
      val json = objectMapper.writeValueAsString(payload)

      val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .timeout(Duration.ofMillis(requestTimeoutMs))
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build()

      httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenAccept { response ->
          if (response.statusCode() in 200..299) {
            if (!collectorAvailable) {
              collectorAvailable = true
              addInfo("Log collector is available again")
            }
          }
        }
        .exceptionally { e ->
          handleError(e)
          null
        }
    } catch (e: Exception) {
      handleError(e)
    }
  }

  private fun handleError(e: Throwable) {
    val now = System.currentTimeMillis()
    if (collectorAvailable || now - lastWarningTime > 60000) {
      addWarn("Failed to send log to collector: ${e.message}")
      collectorAvailable = false
      lastWarningTime = now
    }
  }
}
