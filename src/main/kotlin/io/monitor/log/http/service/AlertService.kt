package io.monitor.log.http.service

import io.monitor.log.http.common.DefaultArgs.DEFAULT_ALERT_THRESHOLD
import io.monitor.log.http.common.TimeMeasure.SECONDS_IN_MINUTE
import io.monitor.log.http.config.ApplicationStreamStartedEvent
import io.monitor.log.http.model.HttpEvent
import io.monitor.log.http.model.after
import io.monitor.log.http.model.before
import io.monitor.log.http.util.parkCurrentThread
import io.monitor.log.http.util.parkMillisCurrentThread
import io.monitor.log.http.util.toFullFormat
import io.monitor.log.http.util.unparkThread
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.ArrayDeque
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque


@Service
class AlertService(
    @Value("\${monitor.events.alert.threshold:$DEFAULT_ALERT_THRESHOLD}")
    private val threshold: Int
) {

    companion object {
        private val log = LoggerFactory.getLogger(AlertService::class.java)

        private const val ALERT_THREAD = "alert-thread"
        private const val WINDOW_TIME = 2 * SECONDS_IN_MINUTE
    }

    @Volatile
    private var handleEvents = true
    private val alertThread = Thread({ handleHttpEvents() }, ALERT_THREAD)

    private var alert = false
    private val buffer: Deque<HttpEvent> = ConcurrentLinkedDeque()
    private val timeWindow: Deque<HttpEvent> = ArrayDeque(WINDOW_TIME.toInt() * threshold)


    @EventListener(ApplicationStreamStartedEvent::class)
    fun startHttpEventHandling() {
        alertThread.start()
        log.info("Alert monitor is started")
    }

    fun addHttpEvent(httpEvent: HttpEvent) {
        log.debug("Alert monitor add event: $httpEvent")
        buffer.addLast(httpEvent)
        unparkThread(alertThread)
    }


    private fun handleHttpEvents() {
        while (handleEvents) {
            var head = buffer.peekFirst()
            while (head == null) {
                parkCurrentThread()
                head = buffer.peekFirst()
            }

            var end = head.timestamp
            var start = end.minusWindowTime()
            logTimeWindow(start, end)
            buffer.pollBefore(end)

            while (timeWindow.isNotEmpty()) {
                alertAction(end)

                end = end.plusSeconds(1)
                start = end.minusWindowTime()
                logTimeWindow(start, end)
                timeWindow.removeBefore(start)
                buffer.pollBefore(end)
            }
            alertAction(end)
        }
    }

    private fun Deque<HttpEvent>.pollBefore(timestamp: ZonedDateTime) {
        while (true) {
            var event = peekFirst()
            if (event == null) {
                parkMillisCurrentThread()
            }
            event = peekFirst()
            if (event == null || event.after(timestamp)) {
                break
            }

            removeFirst()
            timeWindow.addLast(event)
        }
    }

    private fun Deque<HttpEvent>.removeBefore(timestamp: ZonedDateTime) {
        var event = peekFirst()
        while (event.before(timestamp)) {
            removeFirst()
            event = peekFirst()
        }
    }

    private fun ZonedDateTime.minusWindowTime() = minusSeconds(WINDOW_TIME - 1)

    private fun alertAction(timestamp: ZonedDateTime) {
        val newAlert = timeWindow.size > WINDOW_TIME * threshold
        if (!alert && newAlert) {
            log.warn("High traffic generated alert - hits = ${timeWindow.size}, triggered at ${timestamp.toFullFormat()}")
        }
        if (alert && !newAlert) {
            log.info("High traffic recovered - hits = ${timeWindow.size}, triggered at ${timestamp.toFullFormat()}")
        }
        alert = newAlert
    }

    fun logTimeWindow(start: ZonedDateTime, end: ZonedDateTime) {
        log.debug("Time window: ${start.toFullFormat()} -- ${end.toFullFormat()}")
    }
}