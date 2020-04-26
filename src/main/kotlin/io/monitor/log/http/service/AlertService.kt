package io.monitor.log.http.service

import io.monitor.log.http.common.DefaultArgs.DEFAULT_ALERT_THRESHOLD
import io.monitor.log.http.model.HttpEvent
import io.monitor.log.http.model.before
import io.monitor.log.http.model.since
import io.monitor.log.http.util.parkCurrentThread
import io.monitor.log.http.util.parkMillisCurrentThread
import io.monitor.log.http.util.toFullFormat
import io.monitor.log.http.util.unparkThread
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.ArrayDeque
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


@Service
class AlertService(
    @Value("\${monitor.events.alert.threshold:$DEFAULT_ALERT_THRESHOLD}")
    private val threshold: Int
) {

    companion object {
        private val log = LoggerFactory.getLogger(AlertService::class.java)

        private const val ALERT_THREAD = "alert-thread"
        private const val WAKEUP_THREAD = "wakeup-thread"
        private const val WINDOW_TIME = 25L
//        private const val WINDOW_TIME = 2 * SECONDS_IN_MINUTE
    }

    @Volatile
    private var handleEvents = true
    private val alertThread = Thread({ handleHttpEvents() }, ALERT_THREAD)

    private var alert = false
    private val buffer: Deque<HttpEvent> = ConcurrentLinkedDeque()
    private val timeWindow: Deque<HttpEvent> = ArrayDeque(WINDOW_TIME.toInt() * threshold)

    @Volatile
    private var wakeup = false
    private val wakeUpExecutor = Executors.newSingleThreadScheduledExecutor { Thread(it, WAKEUP_THREAD) }


    @EventListener(ApplicationStartedEvent::class)
    fun startHttpEventHandling() {
        alertThread.start()
    }

    fun addHttpEvent(httpEvent: HttpEvent) {
//        log.info("Alert: $httpEvent")
        buffer.addLast(httpEvent)
        unparkThread(alertThread)
    }


    private fun handleHttpEvents() {
        while (handleEvents) {
            log.info("WINDOWSTART: $timeWindow")
            var head = buffer.peekFirst()
            while (head == null) {
                parkCurrentThread()
                head = buffer.peekFirst()
            }

            log.info("NEWEVENT")

            var start = head.timestamp
            var end = start.plusSeconds(WINDOW_TIME)
            log.info("POLLHEAD: $end")
            buffer.pollHeadBefore(end)

            log.info("WINDOW: $timeWindow")

            while (timeWindow.isNotEmpty()) {
                start = start.plusSeconds(1)
                end = start.plusSeconds(WINDOW_TIME)
                timeWindow.removeBefore(start)
                buffer.pollBefore(end)
                alertAction(end)
            }

            alertAction(end) //TODO: need?
        }
    }

    private fun Deque<HttpEvent>.pollBefore(timestamp: ZonedDateTime) {
        while (true) {
            var event = peekFirst()
            if (event == null) {
                parkMillisCurrentThread()
            }
            event = peekFirst()
            if (event == null || event.since(timestamp)) {
                break
            }

            removeFirst()
            timeWindow.addLast(event)
        }
    }

    private fun Deque<HttpEvent>.pollHeadBefore(timestamp: ZonedDateTime) {
        while (true) {
            var event = peekFirst()
            if (event == null) {
                wakeup = false
                val wakeUpExecutor = Executors.newSingleThreadScheduledExecutor { Thread(it, WAKEUP_THREAD) }
                wakeUpExecutor.schedule({ wakeup() }, WINDOW_TIME, TimeUnit.SECONDS)
                log.info("SCHEDULE")
                while (event == null && !wakeup) {
                    log.info("PARK WAKEUP")
                    parkCurrentThread()
                    log.info("UNPARK WAKEUP")
                    event = peekFirst()
                }
                wakeUpExecutor.shutdownNow()
                log.info("LEAVE LOOP")
            }

            if (event == null || event.since(timestamp)) {
                log.info("BREAK")
                break
            }

            log.info("ADD NEW")
            removeFirst()
            timeWindow.addLast(event)
            alertAction(event.timestamp)
        }
    }

    private fun Deque<HttpEvent>.removeBefore(timestamp: ZonedDateTime) {
        var event = peekFirst()
        while (event.before(timestamp)) {
            removeFirst()
            event = peekFirst()
        }
    }

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

    private fun wakeup() {
        log.info("!!!WAKEUP!!!")
        wakeup = true
        unparkThread(alertThread)
    }
}