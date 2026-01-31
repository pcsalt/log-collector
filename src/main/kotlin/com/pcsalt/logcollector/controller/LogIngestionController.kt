package com.pcsalt.logcollector.controller

import com.pcsalt.logcollector.dto.LogEntry
import com.pcsalt.logcollector.dto.LogEntryBatch
import com.pcsalt.logcollector.service.LogService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/logs")
class LogIngestionController(
  private val logService: LogService
) {

  private val logger = LoggerFactory.getLogger(LogIngestionController::class.java)

  @PostMapping
  fun ingestLog(@RequestBody logEntry: LogEntry): ResponseEntity<Map<String, Any>> {
    return try {
      val saved = logService.saveLog(logEntry)
      ResponseEntity.status(HttpStatus.CREATED).body(
        mapOf(
          "status" to "accepted",
          "id" to (saved.id ?: 0)
        )
      )
    } catch (e: Exception) {
      logger.error("Failed to ingest log: {}", e.message)
      ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        mapOf(
          "status" to "error",
          "message" to (e.message ?: "Unknown error")
        )
      )
    }
  }

  @PostMapping("/batch")
  fun ingestLogBatch(@RequestBody batch: LogEntryBatch): ResponseEntity<Map<String, Any>> {
    return try {
      val saved = logService.saveLogs(batch.logs)
      ResponseEntity.status(HttpStatus.CREATED).body(
        mapOf(
          "status" to "accepted",
          "count" to saved.size
        )
      )
    } catch (e: Exception) {
      logger.error("Failed to ingest log batch: {}", e.message)
      ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        mapOf(
          "status" to "error",
          "message" to (e.message ?: "Unknown error")
        )
      )
    }
  }
}
