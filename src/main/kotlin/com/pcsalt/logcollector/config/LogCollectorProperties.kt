package com.pcsalt.logcollector.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "log-collector")
data class LogCollectorProperties(
  val cleanup: CleanupProperties = CleanupProperties(),
  val auth: AuthProperties = AuthProperties()
)

data class CleanupProperties(
  val enabled: Boolean = true,
  val retentionHours: Int = 24,
  val cron: String = "0 0 * * * *"
)

data class AuthProperties(
  val apiKey: String? = null
) {
  fun isEnabled(): Boolean = !apiKey.isNullOrBlank()
  fun isAutoGenerate(): Boolean = apiKey?.lowercase() == "auto"
}
