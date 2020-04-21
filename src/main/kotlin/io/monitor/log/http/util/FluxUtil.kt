package io.monitor.log.http.util

import org.slf4j.Logger
import reactor.core.publisher.Flux

fun <T> Flux<T>.logWith(logger: Logger): Flux<T> = doOnNext { logger.debug("  | Next: $it") }
    .doOnError { logger.error("  | Error: $it") }