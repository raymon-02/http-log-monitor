# http-log-monitor
HTTP log monitor


## Notice

Separate into code notes and structure notes for example

* Timestamp of event: can be no date in log => add local date. May be ok
* Improvement. It's not necessary local file, can be API to get such requests and handle them
* Mix of Spring-Flux high-level framework with implicit thread creation low-level:  
  Spring => log, property, injection, reactive streams  
  Low-level => no blocking during waiting
* JMX/REST to stop handle events
* Log Format: Common Log Format (CLF) (including specifying format of datetime)
* Not from local file but from http => interface HttpEventService
* Only correct timestamp. If super big => loop until this timestamp 
