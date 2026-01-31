package com.pcsalt.logcollector.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("logs")
data class LogEntity(
  @Id
  val id: Long? = null,
  val service: String,
  val level: String,
  val message: String,
  val timestamp: Instant,
  val correlationId: String? = null,
  val logger: String? = null,
  val thread: String? = null,
  val createdAt: Instant = Instant.now()
)
