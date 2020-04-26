package io.monitor.log.http.config

import org.springframework.context.ApplicationEvent

class ApplicationStreamStartedEvent(source: Any) : ApplicationEvent(source)