package com.pcsalt.logcollector.dto

import java.time.Instant

data class LogEntry(
  val service: String,
  val level: String,
  val message: String,
  val timestamp: Instant = Instant.now(),
  val correlationId: String? = null,
  val logger: String? = null,
  val thread: String? = null
)

data class LogEntryBatch(
  val logs: List<LogEntry>
)
