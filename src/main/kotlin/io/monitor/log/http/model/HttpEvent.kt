package io.monitor.log.http.model

import java.time.ZonedDateTime

data class HttpEvent(
    val host: String,
    val section: String,
    val timestamp: ZonedDateTime
)

fun HttpEvent?.before(timestamp: ZonedDateTime) = this != null && this.timestamp.isBefore(timestamp)
fun HttpEvent?.after(timestamp: ZonedDateTime) = this != null && this.timestamp.isAfter(timestamp)