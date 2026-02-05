package com.pcsalt.logcollector.config

import com.pcsalt.logcollector.security.ApiKeyAuthFilter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.io.File
import java.util.UUID

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(LogCollectorProperties::class)
class SecurityConfig(
  private val properties: LogCollectorProperties,
  @Value("\${DATABASE_PATH:./data/logs.db}") private val databasePath: String
) {

  private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)

  private fun getApiKeyFile(): File {
    val dataDir = File(databasePath).parentFile ?: File("./data")
    return File(dataDir, ".api-key")
  }

  @Bean
  fun apiKey(): String {
    val authConfig = properties.auth

    return when {
      !authConfig.isEnabled() -> {
        logger.info("API Key authentication is DISABLED. Set API_KEY to enable.")
        ""
      }
      authConfig.isAutoGenerate() -> {
        val keyFile = getApiKeyFile()
        val key = if (keyFile.exists()) {
          val existingKey = keyFile.readText().trim()
          logger.info("=" .repeat(60))
          logger.info("LOADED API KEY FROM: ${keyFile.absolutePath}")
          logger.info("API KEY: $existingKey")
          logger.info("=" .repeat(60))
          existingKey
        } else {
          val generatedKey = UUID.randomUUID().toString()
          keyFile.parentFile?.mkdirs()
          keyFile.writeText(generatedKey)
          logger.info("=" .repeat(60))
          logger.info("AUTO-GENERATED API KEY: $generatedKey")
          logger.info("Saved to: ${keyFile.absolutePath}")
          logger.info("Use header: X-API-Key: $generatedKey")
          logger.info("=" .repeat(60))
          generatedKey
        }
        key
      }
      else -> {
        logger.info("API Key authentication is ENABLED")
        authConfig.apiKey!!
      }
    }
  }

  @Bean
  fun securityFilterChain(http: HttpSecurity, apiKey: String): SecurityFilterChain {
    http
      .csrf { it.disable() }
      .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
      .cors { }

    if (apiKey.isNotBlank()) {
      http
        .addFilterBefore(ApiKeyAuthFilter(apiKey), UsernamePasswordAuthenticationFilter::class.java)
        .authorizeHttpRequests { auth ->
          auth
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/actuator/**").authenticated()
            .requestMatchers("/api/**").authenticated()
            .requestMatchers("/ws/**").permitAll()
            .anyRequest().permitAll()
        }
    } else {
      http.authorizeHttpRequests { auth ->
        auth.anyRequest().permitAll()
      }
    }

    return http.build()
  }
}
