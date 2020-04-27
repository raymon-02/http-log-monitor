package io.monitor.log.http.common

object DefaultArgs {
    const val DEFAULT_FILE = "/tmp/access.log"
    const val DEFAULT_STATISTIC_PERIOD = 10
    const val DEFAULT_ALERT_THRESHOLD = 10
    const val DEFAULT_WINDOW_TIME = 120
}

object TimeMeasure {
    const val MILLIS_IN_NANOS = 1_000_000L
    const val SECONDS_IN_MINUTE = 60L
}