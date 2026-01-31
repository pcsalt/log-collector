package com.pcsalt.logcollector.service

import com.pcsalt.logcollector.dto.LogEntry
import com.pcsalt.logcollector.dto.LogQueryRequest
import com.pcsalt.logcollector.dto.LogQueryResponse
import com.pcsalt.logcollector.dto.LogResponse
import com.pcsalt.logcollector.dto.LogStatsResponse
import com.pcsalt.logcollector.dto.ServiceStats
import com.pcsalt.logcollector.entity.LogEntity
import com.pcsalt.logcollector.repository.LogQueryRepository
import com.pcsalt.logcollector.repository.LogRepository
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.Timestamp

@Service
class LogService(
  private val logRepository: LogRepository,
  private val logQueryRepository: LogQueryRepository,
  private val logBroadcastService: LogBroadcastService,
  private val jdbcTemplate: JdbcTemplate
) {

  private val logger = LoggerFactory.getLogger(LogService::class.java)

  fun saveLog(logEntry: LogEntry): LogEntity {
    val entity = LogEntity(
      service = logEntry.service,
      level = logEntry.level.uppercase(),
      message = logEntry.message,
      timestamp = logEntry.timestamp,
      correlationId = logEntry.correlationId,
      logger = logEntry.logger,
      thread = logEntry.thread
    )
    val saved = logRepository.save(entity)
    logger.debug("Saved log entry: service={}, level={}", saved.service, saved.level)

    logBroadcastService.broadcast(saved)

    return saved
  }

  fun saveLogs(logEntries: List<LogEntry>): List<LogEntity> {
    if (logEntries.isEmpty()) return emptyList()

    val sql = """
      INSERT INTO logs (service, level, message, timestamp, correlation_id, logger, thread, created_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """.trimIndent()

    val entities = logEntries.map { logEntry ->
      LogEntity(
        service = logEntry.service,
        level = logEntry.level.uppercase(),
        message = logEntry.message,
        timestamp = logEntry.timestamp,
        correlationId = logEntry.correlationId,
        logger = logEntry.logger,
        thread = logEntry.thread
      )
    }

    jdbcTemplate.batchUpdate(sql, entities.map { entity ->
      arrayOf(
        entity.service,
        entity.level,
        entity.message,
        Timestamp.from(entity.timestamp),
        entity.correlationId,
        entity.logger,
        entity.thread,
        Timestamp.from(entity.createdAt)
      )
    })

    logger.debug("Saved {} log entries", entities.size)
    logBroadcastService.broadcastAll(entities)

    return entities
  }

  fun queryLogs(request: LogQueryRequest): LogQueryResponse {
    val logs = logQueryRepository.queryLogs(request)
    val total = logQueryRepository.countLogs(request)
    val hasMore = request.offset + logs.size < total

    return LogQueryResponse(
      logs = logs.map { LogResponse.from(it) },
      total = total,
      limit = request.limit,
      offset = request.offset,
      hasMore = hasMore
    )
  }

  fun getRecentLogs(limit: Int = 100): List<LogEntity> {
    return logRepository.findRecentLogs(limit)
  }

  fun getRecentLogsByService(service: String, limit: Int = 100): List<LogEntity> {
    return logRepository.findRecentLogsByService(service, limit)
  }

  fun getRecentLogsByLevel(level: String, limit: Int = 100): List<LogEntity> {
    return logRepository.findRecentLogsByLevel(level.uppercase(), limit)
  }

  fun getRecentLogsByServiceAndLevel(service: String, level: String, limit: Int = 100): List<LogEntity> {
    return logRepository.findRecentLogsByServiceAndLevel(service, level.uppercase(), limit)
  }

  fun getLogsByCorrelationId(correlationId: String): List<LogEntity> {
    return logRepository.findByCorrelationId(correlationId)
  }

  fun getDistinctServices(): List<String> {
    return logRepository.findDistinctServices()
  }

  fun getStats(): LogStatsResponse {
    val totalLogs = logRepository.countAll()
    val services = logRepository.findDistinctServices()

    val serviceStats = services.map { service ->
      val levels = listOf("DEBUG", "INFO", "WARN", "ERROR")
      val levelCounts = levels.associateWith { level ->
        logRepository.countByServiceAndLevel(service, level)
      }
      ServiceStats(
        service = service,
        count = logRepository.countByService(service),
        levels = levelCounts
      )
    }

    return LogStatsResponse(
      totalLogs = totalLogs,
      services = serviceStats
    )
  }
}
