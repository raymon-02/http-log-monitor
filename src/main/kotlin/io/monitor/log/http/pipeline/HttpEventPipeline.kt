package io.monitor.log.http.pipeline

import io.monitor.log.http.service.AlertService
import io.monitor.log.http.service.HttpEventService
import io.monitor.log.http.service.StatisticService
import io.monitor.log.http.util.logWith
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

@Component
class HttpEventPipeline(
    private val httpEventService: HttpEventService,
    private val statisticService: StatisticService,
    private val alertService: AlertService
) {

    companion object {
        private val log = LoggerFactory.getLogger(HttpEventPipeline::class.java)

        private const val PIPELINE_THREAD = "pipeline-thread"
    }

    @EventListener(ApplicationStartedEvent::class)
    fun pipeline() {
        val httpEventStream = httpEventService.events
            .logWith(log)
            .publishOn(Schedulers.newElastic(PIPELINE_THREAD))

        httpEventStream
            .subscribe { statisticService.addHttpEvent(it) }

        httpEventStream
            .subscribe() { alertService.addHttpEvent(it) }
    }
}