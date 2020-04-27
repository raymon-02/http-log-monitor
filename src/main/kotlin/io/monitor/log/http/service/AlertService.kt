package io.monitor.log.http.service

import io.monitor.log.http.common.DefaultArgs.DEFAULT_ALERT_THRESHOLD
import io.monitor.log.http.common.DefaultArgs.DEFAULT_WINDOW_TIME
import io.monitor.log.http.config.ApplicationStreamStartedEvent
import io.monitor.log.http.model.HttpEvent
import io.monitor.log.http.model.Message
import io.monitor.log.http.model.Priority
import io.monitor.log.http.model.after
import io.monitor.log.http.model.before
import io.monitor.log.http.stream.MessageStream
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
import kotlin.concurrent.thread


@Service
class AlertService(
    @Value("\${monitor.events.alert.threshold:$DEFAULT_ALERT_THRESHOLD}")
    private val threshold: Int,

    @Value("\${monitor.events.alert.window:$DEFAULT_WINDOW_TIME}")
    private val windowTime: Long,

    private val messageStream: MessageStream
) {

    companion object {
        private val log = LoggerFactory.getLogger(AlertService::class.java)

        private const val ALERT_THREAD = "alert-thread"
    }

    @Volatile
    private var handleEvents = true
    private val alertThread = thread(start = false, name = ALERT_THREAD) { handleHttpEvents() }

    private var alert = false
    private val buffer: Deque<HttpEvent> = ConcurrentLinkedDeque()
    private val timeWindow: Deque<HttpEvent> = ArrayDeque(windowTime.toInt() * threshold)


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
            var event = buffer.peekFirst()
            while (event == null) {
                parkCurrentThread()
                event = buffer.peekFirst()
            }

            var end = event.timestamp
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

    private fun ZonedDateTime.minusWindowTime() = minusSeconds(windowTime - 1)

    private fun alertAction(timestamp: ZonedDateTime) {
        val newAlert = timeWindow.size > windowTime * threshold
        if (!alert && newAlert) {
            messageStream.addMessage(
                Message(
                    "High traffic generated alert - hits = ${timeWindow.size}, triggered at ${timestamp.toFullFormat()}",
                    Priority.WARN
                )
            )
        }
        if (alert && !newAlert) {
            messageStream.addMessage(
                Message(
                    "High traffic recovered - hits = ${timeWindow.size}, triggered at ${timestamp.toFullFormat()}",
                    Priority.INFO
                )
            )
        }
        alert = newAlert
    }

    private fun logTimeWindow(start: ZonedDateTime, end: ZonedDateTime) {
        log.debug("Time window: ${start.toFullFormat()} -- ${end.toFullFormat()}")
    }
}