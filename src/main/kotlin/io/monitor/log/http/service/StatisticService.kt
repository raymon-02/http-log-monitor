package io.monitor.log.http.service

import io.monitor.log.http.common.DefaultArgs.DEFAULT_STATISTIC_DURATION
import io.monitor.log.http.model.HttpEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class StatisticService(
    @Value("\${monitor.events.statistic.interval:$DEFAULT_STATISTIC_DURATION}")
    val interval: Int
) {
    companion object {
        private val log = LoggerFactory.getLogger(StatisticService::class.java)
    }

    fun addHttpEvent(httpEvent: HttpEvent) {

    }
}