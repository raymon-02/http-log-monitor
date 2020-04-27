# http-log-monitor

### Overview
**http-log-monitor** is a console solution for monitoring http events and providing simple statistic with alerts.  
The solution gets http events from local file and print statistic with alerts to console.

### Architecture 
**http-log-monitor** is based on [spring-web-flux](https://docs.spring.io/spring-framework/docs/5.0.0.BUILD-SNAPSHOT/spring-framework-reference/html/web-reactive.html)
framework with reactive pipelines 
and can be converted to micro-service providing API for submitting http events reactively.

To provide custom http event source `HttpEventStream` interface should be implemented and added to Spring context
(current `HttpEventStream` implementation  should be excluded from Spring context).

Monitoring implementations use low-level Java thread API to avoid blocking
between reading/writing events from/to reactive pipelines
and keep threads in _park_ state when no events were read from stream
with immediate _unpark_ on new event.

### Events
If default event stream implementation from local file is used then file content must have specific format.

Each event should be placed at its own line and represented with [Common Log Format (CLF)](https://www.w3.org/Daemon/User/Config/Logging.html).  
Example:
```text
127.0.0.1 - james [09/May/2018:16:00:39 +0000] "GET /report HTTP/1.0" 200 123
127.0.0.1 - jill [09/May/2018:16:00:41 +0000] "GET /api/user HTTP/1.0" 200 234
127.0.0.1 - frank [09/May/2018:16:00:42 +0000] "POST /api/user HTTP/1.0" 200 34
127.0.0.1 - mary [09/May/2018:16:00:42 +0000] "POST /api/user HTTP/1.0" 503 12
``` 

### Monitoring
The solution supports two types of monitoring:
* common statistic of http events
* alerts on high traffic

###### Common statistic
Common statistic is provided every statistic time period (10 seconds by default) and includes:
* Total count of http events since the first event
* Delta statistic for the last statistic period:
    * delta events for each host
* Top host statistic:
    * the most requested host with total request count
    * requested sections of the host

Requested section of the host is the first part of the request path:
```text
127.0.0.1 - mary [09/May/2018:16:00:42 +0000] "POST /api/user HTTP/1.0" 503 12
``` 
request path is `/api/user` then requested section is `/api`

Example:
```text
Statistic:
  | Total requests: 378
  |     since 2020-03-09 16:00:39+03:00
  | + 378 requests for the last 10 sec
  |    + 14 : 127.0.0.1
  |    + 364 : 87.240.190.78
  | Top host:
  |   host     = 87.240.190.78
  |   requests = 364
  |   sections = [
  |     /api
  |     /feed
  |     /friends
  |     /info
  |     /test
  |   ]
``` 

###### Alerts
Alerts arise when count of requests for the last two minutes is more than alert threshold (10 req/sec by default).  
When count of requests gets lower than alert threshold notification of traffic normalization is arised.



### Build and start
###### Requirements
* java >= 1.8 (make sure java is added to _$PATH_ environment)

###### Building
Run the following command from the project root:
 ```bash
./gradlew cleand build
```

###### Starting
To start service go to the following folder from the project root: 
```bash
cd build/libs
```
and run
```bash
java -jar http-log-monitor.jar
```

To override default property values additional arguments can be specified during start:
* `--monitor.log.file.name=<file_name>` file with http events (`/tmp/access.log` by default)
* `--monitor.events.statistic.period=<statistic_period>` statistic period (each `10` seconds by default)
* `--monitor.events.alert.threshold=<alert_threshold>` alert threshold (`10` req/sec by default)



### Further possible improvements
* Statistic history flush to storage
* JMX/REST API to start/stop/restart event handling
* API for http event streaming from outside system (via WebSocket)
* Chart for statistic with alerts visualisation
* Detect and handle abnormal events that don't suite current timeline (e.g. events that already happened or events with future time)
* User arguments validation
* Unit and integration tests on all components
