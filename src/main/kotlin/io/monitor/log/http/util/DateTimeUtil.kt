package io.monitor.log.http.util

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


fun ZonedDateTime.toFullFormat(): String = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH:mm:ssXXX")
    .format(this)

fun ZonedDateTime.equalOrAfter(timestamp: ZonedDateTime) = !isBefore(timestamp)
fun ZonedDateTime.before(timestamp: ZonedDateTime) = isBefore(timestamp)