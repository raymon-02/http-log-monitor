package io.monitor.log.http.service

import io.monitor.log.http.common.DefaultArgs.DEFAULT_FILE
import io.monitor.log.http.model.HttpEvent
import io.monitor.log.http.parser.HttpEventParser
import io.monitor.log.http.util.parkMillisCurrentThread
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import reactor.core.publisher.ReplayProcessor
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@Service
class HttpEventService(
    @Value("\${monitor.log.file.name:$DEFAULT_FILE}")
    private val fileName: String,
    private val httpEventParser: HttpEventParser
) {

    companion object {
        private val log = LoggerFactory.getLogger(HttpEventService::class.java)

        private const val EVENT_THREAD = "events-thread"
    }

    @Volatile
    private var readEvents: Boolean = true
    private val eventExecutor: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, EVENT_THREAD) }

    val events: ReplayProcessor<HttpEvent> = ReplayProcessor.create<HttpEvent>(10)


    @EventListener(ApplicationStartedEvent::class)
    fun init() {
        eventExecutor.execute {
            File(fileName).bufferedReader().use { stream ->
                while (readEvents) {
                    stream.readLine()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { httpEventParser.parseHttpEvent(it) }
                        ?.also { events.onNext(it) }
                        ?: parkMillisCurrentThread()
                }
            }
        }
    }
}