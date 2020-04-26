package io.monitor.log.http.util

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


fun ZonedDateTime.toFullFormat(): String = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX")
    .format(this)