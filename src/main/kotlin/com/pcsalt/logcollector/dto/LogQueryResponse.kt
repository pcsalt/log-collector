package com.pcsalt.logcollector.dto

import com.pcsalt.logcollector.entity.LogEntity
import java.time.Instant

data class LogQueryResponse(
  val logs: List<LogResponse>,
  val total: Int,
  val limit: Int,
  val offset: Int,
  val hasMore: Boolean
)

data class LogResponse(
  val id: Long,
  val service: String,
  val level: String,
  val message: String,
  val timestamp: Instant,
  val correlationId: String?,
  val logger: String?,
  val thread: String?,
  val createdAt: Instant
) {
  companion object {
    fun from(entity: LogEntity): LogResponse {
      return LogResponse(
        id = entity.id ?: 0,
        service = entity.service,
        level = entity.level,
        message = entity.message,
        timestamp = entity.timestamp,
        correlationId = entity.correlationId,
        logger = entity.logger,
        thread = entity.thread,
        createdAt = entity.createdAt
      )
    }
  }
}

data class ServiceListResponse(
  val services: List<String>
)

data class LogStatsResponse(
  val totalLogs: Long,
  val services: List<ServiceStats>
)

data class ServiceStats(
  val service: String,
  val count: Long,
  val levels: Map<String, Long>
)
