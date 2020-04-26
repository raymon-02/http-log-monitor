package io.monitor.log.http.model

import io.monitor.log.http.util.before
import io.monitor.log.http.util.equalOrAfter
import java.time.ZonedDateTime

data class HttpEvent(
    val host: String,
    val section: String,
    val timestamp: ZonedDateTime
)

fun HttpEvent?.since(timestamp: ZonedDateTime) = this != null && this.timestamp.equalOrAfter(timestamp)
fun HttpEvent?.before(timestamp: ZonedDateTime) = this != null && this.timestamp.before(timestamp)