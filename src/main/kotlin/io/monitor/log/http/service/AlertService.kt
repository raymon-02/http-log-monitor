package io.monitor.log.http.service

import io.monitor.log.http.model.HttpEvent
import io.monitor.log.http.util.parkCurrentThread
import io.monitor.log.http.util.unparkThread
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.Deque
import java.util.LinkedList
import java.util.concurrent.ConcurrentLinkedDeque


@Service
class AlertService {

    companion object {
        private val log = LoggerFactory.getLogger(AlertService::class.java)

        private const val ALERT_THREAD = "alert-thread"
    }

    private val alertThread = Thread({ handleHttpEvents() }, ALERT_THREAD)

    private val events: Deque<HttpEvent> = ConcurrentLinkedDeque()
    private val timeWindow: MutableList<HttpEvent> = LinkedList()


    @EventListener(ApplicationStartedEvent::class)
    fun startHttpEventHandling() {
        alertThread.start()
    }

    fun addHttpEvent(httpEvent: HttpEvent) {
        log.info("Alert: $httpEvent")
        events.addLast(httpEvent)
        unparkThread(alertThread)
    }


    private fun handleHttpEvents() {
        while (true) {
            if (timeWindow.isEmpty()) {
                parkCurrentThread()
            }
        }
    }
}