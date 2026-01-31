package com.pcsalt.logcollector.service

import com.pcsalt.logcollector.dto.LogBroadcast
import com.pcsalt.logcollector.entity.LogEntity
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class LogBroadcastService(
  private val messagingTemplate: SimpMessagingTemplate
) {

  private val logger = LoggerFactory.getLogger(LogBroadcastService::class.java)

  fun broadcast(logEntity: LogEntity) {
    val broadcast = LogBroadcast.from(logEntity)

    // Broadcast to all subscribers
    messagingTemplate.convertAndSend("/topic/logs", broadcast)

    // Broadcast to service-specific topic
    messagingTemplate.convertAndSend("/topic/logs/${logEntity.service}", broadcast)

    // Broadcast to level-specific topic
    messagingTemplate.convertAndSend("/topic/logs/level/${logEntity.level}", broadcast)

    logger.debug("Broadcast log: service={}, level={}", logEntity.service, logEntity.level)
  }

  fun broadcastAll(logEntities: List<LogEntity>) {
    logEntities.forEach { broadcast(it) }
  }
}
