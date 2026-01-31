package com.pcsalt.logcollector.repository

import com.pcsalt.logcollector.entity.LogEntity
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface LogRepository : CrudRepository<LogEntity, Long> {

  fun findByServiceOrderByTimestampDesc(service: String): List<LogEntity>

  fun findByLevelOrderByTimestampDesc(level: String): List<LogEntity>

  @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit")
  fun findRecentLogs(limit: Int): List<LogEntity>

  @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
  fun findRecentLogsWithOffset(limit: Int, offset: Int): List<LogEntity>

  @Query("SELECT * FROM logs WHERE service = :service ORDER BY timestamp DESC LIMIT :limit")
  fun findRecentLogsByService(service: String, limit: Int): List<LogEntity>

  @Query("SELECT * FROM logs WHERE level = :level ORDER BY timestamp DESC LIMIT :limit")
  fun findRecentLogsByLevel(level: String, limit: Int): List<LogEntity>

  @Query("SELECT * FROM logs WHERE service = :service AND level = :level ORDER BY timestamp DESC LIMIT :limit")
  fun findRecentLogsByServiceAndLevel(service: String, level: String, limit: Int): List<LogEntity>

  @Query("SELECT * FROM logs WHERE message LIKE :pattern ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
  fun searchByMessage(pattern: String, limit: Int, offset: Int): List<LogEntity>

  @Query("SELECT * FROM logs WHERE correlation_id = :correlationId ORDER BY timestamp DESC")
  fun findByCorrelationId(correlationId: String): List<LogEntity>

  @Query("SELECT * FROM logs WHERE timestamp >= :from AND timestamp <= :to ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
  fun findByTimeRange(from: Instant, to: Instant, limit: Int, offset: Int): List<LogEntity>

  @Query("SELECT DISTINCT service FROM logs ORDER BY service")
  fun findDistinctServices(): List<String>

  @Query("SELECT COUNT(*) FROM logs")
  fun countAll(): Long

  @Query("SELECT COUNT(*) FROM logs WHERE service = :service")
  fun countByService(service: String): Long

  @Query("SELECT COUNT(*) FROM logs WHERE service = :service AND level = :level")
  fun countByServiceAndLevel(service: String, level: String): Long

  @Modifying
  @Query("DELETE FROM logs WHERE created_at < :cutoff")
  fun deleteLogsOlderThan(cutoff: Instant): Int
}
