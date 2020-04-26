package io.monitor.log.http.util

import io.monitor.log.http.common.TimeMeasure.MILLIS_IN_NANOS
import java.util.concurrent.locks.LockSupport

fun parkCurrentThread() {
    LockSupport.park()
    if (Thread.interrupted()) {
        throw InterruptedException()
    }
}

fun parkMillisCurrentThread(millis: Long = 1000) {
    LockSupport.parkNanos(millis * MILLIS_IN_NANOS)
    if (Thread.interrupted()) {
        throw InterruptedException()
    }
}

fun unparkThread(thread: Thread) = LockSupport.unpark(thread)