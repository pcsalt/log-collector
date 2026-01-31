package com.pcsalt.logcollector.controller

import com.pcsalt.logcollector.service.LogService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class DashboardController(
  private val logService: LogService
) {

  @GetMapping("/")
  fun dashboard(model: Model): String {
    val services = logService.getDistinctServices()
    model.addAttribute("services", services)
    return "dashboard"
  }
}
