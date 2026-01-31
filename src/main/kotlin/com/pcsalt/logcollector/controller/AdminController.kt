package com.pcsalt.logcollector.controller

import com.pcsalt.logcollector.service.LogCleanupService
import com.pcsalt.logcollector.service.LogService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class AdminController(
  private val logCleanupService: LogCleanupService,
  private val logService: LogService
) {

  @DeleteMapping("/cleanup")
  fun triggerCleanup(
    @RequestParam(defaultValue = "24") retentionHours: Int
  ): ResponseEntity<Map<String, Any>> {
    val deletedCount = logCleanupService.manualCleanup(retentionHours)
    return ResponseEntity.ok(
      mapOf(
        "status" to "completed",
        "deletedCount" to deletedCount,
        "retentionHours" to retentionHours
      )
    )
  }

  @GetMapping("/stats")
  fun getStats(): ResponseEntity<Map<String, Any>> {
    val services = logService.getDistinctServices()
    return ResponseEntity.ok(
      mapOf(
        "services" to services,
        "serviceCount" to services.size
      )
    )
  }
}
