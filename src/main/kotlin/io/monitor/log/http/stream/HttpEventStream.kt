package io.monitor.log.http.stream

import io.monitor.log.http.model.HttpEvent
import reactor.core.publisher.Flux

interface HttpEventStream {
    val events: Flux<HttpEvent>
}