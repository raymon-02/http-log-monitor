package io.monitor.log.http.service

import io.monitor.log.http.model.HttpEvent
import io.monitor.log.http.model.Message
import io.monitor.log.http.stream.MessageStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [AlertService::class, TestMessageStream::class])
@TestPropertySource(
    properties = [
        "monitor.events.alert.threshold=1",
        "monitor.events.alert.window=20"
    ]
)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class AlertServiceTest {

    private val host = "127.0.0.1"
    private val section = "/test"

    @Value("\${monitor.events.alert.window}")
    private var windowTime = 0L

    @Autowired
    private lateinit var alertService: AlertService

    @Autowired
    private lateinit var messageStream: TestMessageStream

    @Before
    fun setUp() {
        alertService.startHttpEventHandling()
    }

    @Test
    fun `should arise alert on high traffic`() {
        (0..11).forEach {
            alertService.addHttpEvent(event(20, 20, it))
            alertService.addHttpEvent(event(20, 20, it))
        }

        checkWithAttempts {
            assertThat(messageStream.getCount()).isEqualTo(1)
        }
    }

    @Test
    fun `should recover after high traffic`() {
        (0..11).forEach {
            alertService.addHttpEvent(event(23, 59, it))
            alertService.addHttpEvent(event(23, 59, it))
        }
        timeout(1000 * windowTime)

        checkWithAttempts {
            assertThat(messageStream.getCount()).isEqualTo(2)
        }
    }

    private fun event(hours: Int, minutes: Int, seconds: Int) = HttpEvent(host, section, time(hours, minutes, seconds))
}

@Component
private class TestMessageStream : MessageStream {
    private val count = AtomicInteger(0)

    override fun addMessage(message: Message) {
        count.incrementAndGet()
    }

    fun getCount() = count.get()
}


private fun time(hours: Int, minutes: Int, seconds: Int) =
    ZonedDateTime.of(
        2020, 3, 3, hours, minutes, seconds, 0, ZoneId.systemDefault()
    )

private fun timeout(millis: Long = 1000) = Thread.sleep(millis)

private fun checkWithAttempts(attempts: Int = 3, millis: Long = 1000, assertion: () -> Unit) {
    var failed = true
    var throwable: Throwable = AssertionError()
    var count = 0
    val checkedAttempts = max(attempts, 1)
    while (failed && count < checkedAttempts) {
        try {
            assertion()
            failed = false
        } catch (th: Throwable) {
            throwable = th
            count += 1
            timeout(millis)
        }
    }
    if (failed) {
        throw throwable
    }
}