package com.pcsalt.logcollector.service

import com.pcsalt.logcollector.config.LogCollectorProperties
import com.pcsalt.logcollector.repository.LogRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class LogCleanupService(
  private val logRepository: LogRepository,
  private val properties: LogCollectorProperties
) {

  private val logger = LoggerFactory.getLogger(LogCleanupService::class.java)

  @Scheduled(cron = "\${log-collector.cleanup.cron:0 0 * * * *}")
  fun cleanupOldLogs() {
    if (!properties.cleanup.enabled) {
      logger.debug("Log cleanup is disabled")
      return
    }

    val retentionHours = properties.cleanup.retentionHours
    val cutoff = Instant.now().minus(retentionHours.toLong(), ChronoUnit.HOURS)

    logger.info("Starting log cleanup, deleting logs older than {} hours (before {})", retentionHours, cutoff)

    try {
      val deletedCount = logRepository.deleteLogsOlderThan(cutoff)
      logger.info("Log cleanup completed, deleted {} log entries", deletedCount)
    } catch (e: Exception) {
      logger.error("Log cleanup failed: {}", e.message, e)
    }
  }

  fun manualCleanup(retentionHours: Int = properties.cleanup.retentionHours): Int {
    val cutoff = Instant.now().minus(retentionHours.toLong(), ChronoUnit.HOURS)
    logger.info("Manual cleanup triggered, deleting logs before {}", cutoff)
    return logRepository.deleteLogsOlderThan(cutoff)
  }
}
