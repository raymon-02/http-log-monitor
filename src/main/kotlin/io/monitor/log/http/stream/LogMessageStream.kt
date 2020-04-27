package io.monitor.log.http.stream

import io.monitor.log.http.config.ApplicationStreamStartedEvent
import io.monitor.log.http.model.Message
import io.monitor.log.http.model.Priority.INFO
import io.monitor.log.http.model.Priority.WARN
import io.monitor.log.http.util.parkCurrentThread
import io.monitor.log.http.util.unparkThread
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.concurrent.thread

@Component
class LogMessageStream : MessageStream {

    companion object {
        private val log = LoggerFactory.getLogger(LogMessageStream::class.java)

        private const val MESSAGE_THREAD = "message-thread"
    }

    @Volatile
    private var handleMessages = true

    private val messages: Deque<Message> = ConcurrentLinkedDeque()
    private val messageThread = thread(start = false, name = MESSAGE_THREAD) { handleMessageEvents() }


    @EventListener(ApplicationStreamStartedEvent::class)
    fun startMessageHandling() {
        messageThread.start()
        log.info("Message handling started")
    }

    override fun addMessage(message: Message) {
        messages.addLast(message)
        unparkThread(messageThread)
    }

    private fun handleMessageEvents() {
        while (handleMessages) {
            var message = messages.pollFirst()
            while (message == null) {
                parkCurrentThread()
                message = messages.pollFirst()
            }

            logMessage(message)
        }
    }

    private fun logMessage(message: Message) {
        when (message.priority) {
            INFO -> log.info(message.message)
            WARN -> log.warn(message.message)
        }
    }

}