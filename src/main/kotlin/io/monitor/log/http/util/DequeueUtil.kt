package io.monitor.log.http.util

import java.util.Deque

/**
 * This method is thread-safe if there is only one reader of receiver collection
 */
fun <T> Deque<T>.pollLastInclusive(): List<T> {
    val last = peekLast() ?: return emptyList()
    val result = mutableListOf<T>()
    while (true) {
        val current = pollFirst() ?: break
        result.add(current)
        if (current === last) {
            break
        }
    }
    return result
}