package io.monitor.log.http.stream

import io.monitor.log.http.common.DefaultArgs.DEFAULT_FILE
import io.monitor.log.http.config.ApplicationStreamStartedEvent
import io.monitor.log.http.model.HttpEvent
import io.monitor.log.http.parser.HttpEventParser
import io.monitor.log.http.util.parkMillisCurrentThread
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import reactor.core.publisher.ReplayProcessor
import java.io.File
import java.util.concurrent.Executors


@Service
class FileHttpEventStream(
    @Value("\${monitor.log.file.name:$DEFAULT_FILE}")
    private val fileName: String,
    private val httpEventParser: HttpEventParser,
    private val eventPublisher: ApplicationEventPublisher
) : HttpEventStream {

    companion object {
        private val log = LoggerFactory.getLogger(FileHttpEventStream::class.java)

        private const val EVENT_THREAD = "events-thread"
        private const val EVENTS_HISTORY_REPLAY_SIZE = 10000
    }

    @Volatile
    private var readEvents = true
    private val eventExecutor = Executors.newSingleThreadExecutor { Thread(it, EVENT_THREAD) }

    override val events: ReplayProcessor<HttpEvent> = ReplayProcessor.create<HttpEvent>(EVENTS_HISTORY_REPLAY_SIZE)

    @EventListener(ApplicationStartedEvent::class)
    fun init() {
        eventExecutor.execute {
            log.info("File reader is starting")
            File(fileName).bufferedReader().use { stream ->
                log.info("File reader is started")
                eventPublisher.publishEvent(ApplicationStreamStartedEvent(eventPublisher))
                while (readEvents) {
                    val line = stream.readLine()
                    if (line == null) {
                        parkMillisCurrentThread()
                    } else {
                        line.takeIf { it.isNotBlank() }
                            ?.let { httpEventParser.parseHttpEvent(it) }
                            ?.also { events.onNext(it) }
                    }
                }
            }
        }
    }
}