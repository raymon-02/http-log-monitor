package io.monitor.log.http.parser

import io.monitor.log.http.model.HttpEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Service
class HttpEventParser {

    companion object {
        private val log = LoggerFactory.getLogger(HttpEventParser::class.java)

        // 1:IP 2:client 3:user 4:datetime 5:method 6:req 7:proto 8:respcode 9:size
        private val EVENT_REGEX =
            "^(\\S+) (\\S+) (\\S+) \\[([\\w:/]+\\s[+\\-]\\d{4})] \"(\\S+) (\\S+) (\\S+)\" (\\d{3}) (\\d+)".toRegex()
        private const val HOST_GROUP = 1
        private const val REQUEST_GROUP = 6
        private const val DATETIME_GROUP = 4
    }

    fun parseHttpEvent(content: String): HttpEvent? {
        val matchResult = EVENT_REGEX.matchEntire(content)
        return if (matchResult != null) {
            HttpEvent(
                matchResult.groupValues[HOST_GROUP],
                matchResult.groupValues[REQUEST_GROUP].firstSection(),
                matchResult.groupValues[DATETIME_GROUP].toLocalDateTime()
            )
        } else {
            log.warn("Http event from log line '$content' cannot be parsed")
            null
        }
    }
}


private fun String.toLocalDateTime() = LocalDateTime.parse(
    this,
    DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss X")
)

private fun String.firstSection() = "/${substring(1).substringBefore("/")}"