package io.monitor.log.http

import io.monitor.log.http.common.DefaultArgs
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener


//TODO: debug logging in all classes

@SpringBootApplication
class HttpMonitorLogApplication(
    @Value("\${monitor.log.file.name:${DefaultArgs.DEFAULT_FILE}}")
    private val logFileName: String,

    @Value("\${monitor.events.statistic.interval:${DefaultArgs.DEFAULT_STATISTIC_DURATION}}")
    val interval: Int
) {

    companion object {
        private val log = LoggerFactory.getLogger(HttpMonitorLogApplication::class.java)
    }

    @EventListener(ContextRefreshedEvent::class)
    fun logArgs(
    ) {
        val args = buildString {
            append("\n")
            append("Application has been started with args:").append("\n")
            append("    Log file name      = $logFileName").append("\n")
            append("    Statistic interval = $interval").append("\n")
            append("    Alert threshold    = ????????")
        }
        log.info(args)
    }
}

fun main(args: Array<String>) {
    runApplication<HttpMonitorLogApplication>(*args)
}
