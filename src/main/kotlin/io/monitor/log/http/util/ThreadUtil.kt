package io.monitor.log.http.util

import java.util.concurrent.locks.LockSupport

fun parkCurrentThread() {
    LockSupport.park()
    if (Thread.interrupted()) {
        throw InterruptedException()
    }
}

fun parkMillisCurrentThread(millis: Long = 1000) {
    LockSupport.parkNanos(millis * 1_000_000)
    if (Thread.interrupted()) {
        throw InterruptedException()
    }
}

fun unparkThread(thread: Thread) = LockSupport.unpark(thread)