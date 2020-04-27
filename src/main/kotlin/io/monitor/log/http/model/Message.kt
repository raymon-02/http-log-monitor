package io.monitor.log.http.model

data class Message(
    val message: String,
    val priority: Priority
)

enum class Priority {
    INFO,
    WARN
}