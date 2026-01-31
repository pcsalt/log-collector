package com.pcsalt.logcollector

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
class LogCollectorApplication

fun main(args: Array<String>) {
  runApplication<LogCollectorApplication>(*args)
}
