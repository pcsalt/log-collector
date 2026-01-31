package com.pcsalt.logcollector.repository

import com.pcsalt.logcollector.dto.LogQueryRequest
import com.pcsalt.logcollector.entity.LogEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant

@Repository
class LogQueryRepository(
  private val jdbcTemplate: JdbcTemplate
) {

  private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
    LogEntity(
      id = rs.getLong("id"),
      service = rs.getString("service"),
      level = rs.getString("level"),
      message = rs.getString("message"),
      timestamp = rs.getTimestamp("timestamp").toInstant(),
      correlationId = rs.getString("correlation_id"),
      logger = rs.getString("logger"),
      thread = rs.getString("thread"),
      createdAt = rs.getTimestamp("created_at").toInstant()
    )
  }

  fun queryLogs(request: LogQueryRequest): List<LogEntity> {
    val sql = StringBuilder("SELECT * FROM logs WHERE 1=1")
    val params = mutableListOf<Any>()

    request.service?.let {
      sql.append(" AND service = ?")
      params.add(it)
    }

    request.level?.let {
      sql.append(" AND level = ?")
      params.add(it.uppercase())
    }

    request.levels?.takeIf { it.isNotEmpty() }?.let { levels ->
      val placeholders = levels.joinToString(",") { "?" }
      sql.append(" AND level IN ($placeholders)")
      params.addAll(levels.map { it.uppercase() })
    }

    request.search?.let {
      sql.append(" AND message LIKE ?")
      params.add("%$it%")
    }

    request.correlationId?.let {
      sql.append(" AND correlation_id = ?")
      params.add(it)
    }

    request.from?.let {
      sql.append(" AND timestamp >= ?")
      params.add(java.sql.Timestamp.from(it))
    }

    request.to?.let {
      sql.append(" AND timestamp <= ?")
      params.add(java.sql.Timestamp.from(it))
    }

    sql.append(" ORDER BY timestamp DESC")
    sql.append(" LIMIT ? OFFSET ?")
    params.add(request.limit)
    params.add(request.offset)

    return jdbcTemplate.query(sql.toString(), rowMapper, *params.toTypedArray())
  }

  fun countLogs(request: LogQueryRequest): Int {
    val sql = StringBuilder("SELECT COUNT(*) FROM logs WHERE 1=1")
    val params = mutableListOf<Any>()

    request.service?.let {
      sql.append(" AND service = ?")
      params.add(it)
    }

    request.level?.let {
      sql.append(" AND level = ?")
      params.add(it.uppercase())
    }

    request.levels?.takeIf { it.isNotEmpty() }?.let { levels ->
      val placeholders = levels.joinToString(",") { "?" }
      sql.append(" AND level IN ($placeholders)")
      params.addAll(levels.map { it.uppercase() })
    }

    request.search?.let {
      sql.append(" AND message LIKE ?")
      params.add("%$it%")
    }

    request.correlationId?.let {
      sql.append(" AND correlation_id = ?")
      params.add(it)
    }

    request.from?.let {
      sql.append(" AND timestamp >= ?")
      params.add(java.sql.Timestamp.from(it))
    }

    request.to?.let {
      sql.append(" AND timestamp <= ?")
      params.add(java.sql.Timestamp.from(it))
    }

    return jdbcTemplate.queryForObject(sql.toString(), Int::class.java, *params.toTypedArray()) ?: 0
  }
}
