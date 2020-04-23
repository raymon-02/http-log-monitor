package io.monitor.log.http.model


data class HostHistory(
    var hits: Long,
    val sections: MutableSet<String>
)

data class Delta(
    val count: Int,
    val hostDelta: Map<String, HostDelta>
)

data class HostDelta(
    val count: Int,
    val sections: Set<String>
)


data class TopHost(
    val host: String,
    val hits: Long,
    val sections: Set<String>
)