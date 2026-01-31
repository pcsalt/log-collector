package com.pcsalt.logcollector.dto

import java.time.Instant

data class LogQueryRequest(
  val service: String? = null,
  val level: String? = null,
  val levels: List<String>? = null,
  val search: String? = null,
  val correlationId: String? = null,
  val from: Instant? = null,
  val to: Instant? = null,
  val limit: Int = 100,
  val offset: Int = 0
)
