package io.monitor.log.http.stream

import io.monitor.log.http.model.Message

interface MessageStream {
    fun addMessage(message: Message)
}