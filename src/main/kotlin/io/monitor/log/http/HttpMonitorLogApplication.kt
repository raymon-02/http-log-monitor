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

    @Value("\${monitor.events.statistic.period:${DefaultArgs.DEFAULT_STATISTIC_PERIOD}}")
    val statisticPeriod: Long
) {

    companion object {
        private val log = LoggerFactory.getLogger(HttpMonitorLogApplication::class.java)
    }

    @EventListener(ContextRefreshedEvent::class)
    fun logArgs(
    ) {
        val args = buildString {
            append("\n")
            append("Application is starting with args:").append("\n")
            append("    Log file name              = $logFileName").append("\n")
            append("    Statistic period (seconds) = $statisticPeriod").append("\n")
            append("    Alert threshold (req/sec)  = ????????")
        }
        log.info(args)
    }
}

fun main(args: Array<String>) {
    runApplication<HttpMonitorLogApplication>(*args)
}
