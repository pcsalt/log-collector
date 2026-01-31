package com.pcsalt.logcollector.controller

import com.pcsalt.logcollector.dto.LogQueryRequest
import com.pcsalt.logcollector.dto.LogQueryResponse
import com.pcsalt.logcollector.dto.LogResponse
import com.pcsalt.logcollector.dto.LogStatsResponse
import com.pcsalt.logcollector.dto.ServiceListResponse
import com.pcsalt.logcollector.service.LogService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/logs")
class LogQueryController(
  private val logService: LogService
) {

  @GetMapping
  fun queryLogs(
    @RequestParam(required = false) service: String?,
    @RequestParam(required = false) level: String?,
    @RequestParam(required = false) levels: List<String>?,
    @RequestParam(required = false) search: String?,
    @RequestParam(required = false) correlationId: String?,
    @RequestParam(required = false) from: Instant?,
    @RequestParam(required = false) to: Instant?,
    @RequestParam(defaultValue = "100") limit: Int,
    @RequestParam(defaultValue = "0") offset: Int
  ): ResponseEntity<LogQueryResponse> {
    val request = LogQueryRequest(
      service = service,
      level = level,
      levels = levels,
      search = search,
      correlationId = correlationId,
      from = from,
      to = to,
      limit = limit.coerceIn(1, 500),
      offset = offset.coerceAtLeast(0)
    )
    val response = logService.queryLogs(request)
    return ResponseEntity.ok(response)
  }

  @GetMapping("/correlation/{correlationId}")
  fun getByCorrelationId(
    @PathVariable correlationId: String
  ): ResponseEntity<List<LogResponse>> {
    val logs = logService.getLogsByCorrelationId(correlationId)
    return ResponseEntity.ok(logs.map { LogResponse.from(it) })
  }

  @GetMapping("/services")
  fun getServices(): ResponseEntity<ServiceListResponse> {
    val services = logService.getDistinctServices()
    return ResponseEntity.ok(ServiceListResponse(services))
  }

  @GetMapping("/stats")
  fun getStats(): ResponseEntity<LogStatsResponse> {
    val stats = logService.getStats()
    return ResponseEntity.ok(stats)
  }
}
