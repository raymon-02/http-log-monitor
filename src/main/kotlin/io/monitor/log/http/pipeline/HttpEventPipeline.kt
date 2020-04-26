package io.monitor.log.http.pipeline

import io.monitor.log.http.config.ApplicationStreamStartedEvent
import io.monitor.log.http.service.AlertService
import io.monitor.log.http.service.StatisticService
import io.monitor.log.http.stream.HttpEventStream
import io.monitor.log.http.util.logWith
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

@Component
class HttpEventPipeline(
    private val httpEventStream: HttpEventStream,
    private val statisticService: StatisticService,
    private val alertService: AlertService
) {

    companion object {
        private val log = LoggerFactory.getLogger(HttpEventPipeline::class.java)

        private const val PIPELINE_THREAD = "pipeline-thread"
    }

    @EventListener(ApplicationStreamStartedEvent::class)
    fun pipeline() {
        log.info("Event pipeline is started")
        val httpEventStream = httpEventStream.events
            .publishOn(Schedulers.newElastic(PIPELINE_THREAD))
            .logWith(log)

        httpEventStream
            .subscribe { statisticService.addHttpEvent(it) }

        httpEventStream
            .subscribe { alertService.addHttpEvent(it) }
    }
}