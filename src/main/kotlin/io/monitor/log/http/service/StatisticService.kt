package io.monitor.log.http.service

import io.monitor.log.http.common.DefaultArgs.DEFAULT_STATISTIC_PERIOD
import io.monitor.log.http.model.Delta
import io.monitor.log.http.model.HostDelta
import io.monitor.log.http.model.HostHistory
import io.monitor.log.http.model.HttpEvent
import io.monitor.log.http.model.TopHost
import io.monitor.log.http.util.pollLastInclusive
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


@Service
class StatisticService(
    @Value("\${monitor.events.statistic.period:$DEFAULT_STATISTIC_PERIOD}")
    val period: Long
) {
    companion object {
        private val log = LoggerFactory.getLogger(StatisticService::class.java)

        private const val STATISTIC_THREAD = "statistic-thread"
    }

    private val statisticScheduler = Executors.newSingleThreadScheduledExecutor { Thread(it, STATISTIC_THREAD) }

    private var totalEvents = 0L
    private var firstEventTimestamp: LocalDateTime? = null

    private val buffer = ConcurrentLinkedDeque<HttpEvent>()
    private val history = mutableMapOf<String, HostHistory>()


    fun addHttpEvent(httpEvent: HttpEvent) {
        buffer.addLast(httpEvent)
    }

    @EventListener(ApplicationStartedEvent::class)
    fun scheduleStatisticCollection() {
        statisticScheduler.scheduleAtFixedRate({ collectStatistic() }, period, period, TimeUnit.SECONDS)
    }

    private fun collectStatistic() {
        val newEvents = buffer.pollLastInclusive()
        updateCommonStatistic(newEvents)
        val delta = getDelta(newEvents)
        val topHost = updateHistoryAndGetTopHost(delta)

        log.info(createStatisticLog(delta, topHost))
    }

    private fun createStatisticLog(delta: Delta, topHost: TopHost?) =
        buildString {
            append("\n")
            append("Statistic:").append("\n")
            append("  | Total requests: $totalEvents").append("\n")
            append("  |     since $firstEventTimestamp").append("\n")
            append("  | + ${delta.count} requests for the last $period sec").append("\n")
            delta.hostDelta.forEach { (host, hostDelta) ->
                append("  |    + ${hostDelta.count} : $host").append("\n")
            }
            if (topHost != null) {
                append("  | Top host:").append("\n")
                append("  |   host     = ${topHost.host}").append("\n")
                append("  |   requests = ${topHost.hits}").append("\n")
                append("  |   sections = [").append("\n")
                topHost.sections.forEach {
                    append("  |     $it").append("\n")
                }
                append("  |   ]")
            }
        }

    private fun updateCommonStatistic(events: List<HttpEvent>) {
        totalEvents += events.size
        if (firstEventTimestamp == null) {
            firstEventTimestamp = events.takeIf { it.isNotEmpty() }?.first()?.timestamp
        }
    }

    private fun getDelta(events: List<HttpEvent>) =
        Delta(
            events.size,
            events.groupBy({ it.host }, { it.section })
                .mapValues { (_, sections) ->
                    HostDelta(
                        sections.size,
                        sections.toSet()
                    )
                }
        )

    private fun updateHistoryAndGetTopHost(delta: Delta): TopHost? {
        delta.hostDelta.forEach { (host, hostDelta) ->
            history.getOrPut(host) { HostHistory(0, mutableSetOf()) }
                .let {
                    it.hits += hostDelta.count
                    it.sections.addAll(hostDelta.sections)
                }
        }

        return history.maxBy { (_, hostHistory) ->
            hostHistory.hits
        }?.let { (host, hostHistory) ->
            TopHost(host, hostHistory.hits, hostHistory.sections)
        }
    }
}
