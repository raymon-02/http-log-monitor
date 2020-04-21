package io.monitor.log.http.model

import java.time.LocalDateTime

data class HttpEvent(
    val host: String,
    val section: String,
    val timestamp: LocalDateTime
)