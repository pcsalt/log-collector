package com.pcsalt.logcollector.dto

import com.pcsalt.logcollector.entity.LogEntity
import java.time.Instant

data class LogBroadcast(
  val id: Long,
  val service: String,
  val level: String,
  val message: String,
  val timestamp: Instant,
  val correlationId: String?,
  val logger: String?,
  val thread: String?
) {
  companion object {
    fun from(entity: LogEntity): LogBroadcast {
      return LogBroadcast(
        id = entity.id ?: 0,
        service = entity.service,
        level = entity.level,
        message = entity.message,
        timestamp = entity.timestamp,
        correlationId = entity.correlationId,
        logger = entity.logger,
        thread = entity.thread
      )
    }
  }
}
