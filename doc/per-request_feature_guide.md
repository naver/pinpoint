---
title: Separate Logging Per Request
keywords: history
last_updated: Feb 1, 2018
sidebar: mydoc_sidebar
permalink: perrequestfeatureguide.html
disqus: false
---

# ENGLISH GUIDE

## Per-request logging

### 1. Description 
Pinpoint saves additional information(transactionId, spanId) in log messages to classify them by request.

When tomcat processes multiple requests concurrently, we can see log files printed in chronological order.
But we can not classify them by each request.
For example when an exception message is logged, we can not easily identify all the logs related to the request that threw the exception.

Pinpoint is able to classify logs by requests by storing additional information(transactionId, spanId) in MDC of each request.
The transactionId printed in the log message is the same as the transactionId in Pinpoint Web’s Transaction List view.
 
Let’s take a look at a more specific example.
The log below is from an exception that occurred without using Pinpoint. 
As you can see, it is hard to identify the logs related to the request that threw the exception.
ex) Without Pinpoint
```
2015-04-04 14:35:20 [INFO](ContentInfoCollector:76 ) get content name : TECH
2015-04-04 14:35:20 [INFO](ContentInfoCollector:123 ) get content name : OPINION
2015-04-04 14:35:20 [INFO](ContentInfoCollector:12) get content name : SPORTS
2015-04-04 14:35:20 [INFO](ContentInfoCollector:25 ) get content name : TECH
2015-04-04 14:35:20 [INFO](ContentInfoCollector:56 ) get content name : NATIONAL
2015-04-04 14:35:20 [INFO](ContentInfoCollector:34 ) get content name : OPINION
2015-04-04 14:35:20 [INFO](ContentInfoService:55 ) check authorization of user
2015-04-04 14:35:20 [INFO](ContentInfoService:14 ) get title of content
2015-04-04 14:35:21 [INFO](ContentDAOImpl:14 ) execute query ...
2015-04-04 14:35:21 [INFO](ContentDAOImpl:114 ) execute query ...    
2015-04-04 14:35:20 [INFO](ContentInfoService:74 ) get top linking for content
2015-04-04 14:35:21 [INFO](ContentDAOImpl:14 ) execute query ...
2015-04-04 14:35:21 [INFO](ContentDAOImpl:114 ) execute query ...
2015-04-04 14:35:22 [INFO](ContentDAOImpl:186 ) execute query ...
2015-04-04 14:35:22 [ERROR]ContentDAOImpl:158 )
     com.mysql.jdbc.exceptions.jdbc4.CommunicationsException: Communications link failure
          at example.ContentDAO.executequery(ContentDAOImpl.java:152)
          ...
          at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
          at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
          at com.mysql.jdbc.ConnectionImpl.<init>(ConnectionImpl.java:787)
          ...
     Caused by: com.mysql.jdbc.exceptions.jdbc4.CommunicationsException:
     Communications link failure The last packet sent successfully to the server was 0 milliseconds ago.
     The driver has not received any packets from the server.    
          at com.mysql.jdbc.ConnectionImpl.createNewIO(ConnectionImpl.java:2181)
          ... 12 more
     Caused by: java.net.ConnectException: Connection refused
          at java.net.PlainSocketImpl.socketConnect(Native Method)
          at java.net.PlainSocketImpl.doConnect(PlainSocketImpl.java:333)
          at java.net.PlainSocketImpl.connectToAddress(PlainSocketImpl.java:195)   
          at java.net.PlainSocketImpl.connect(PlainSocketImpl.java:182)   
          at java.net.SocksSocketImpl.connect(SocksSocketImpl.java:432)   
          at java.net.Socket.connect(Socket.java:529)    
          ...13 more
2015-04-04 14:35:22 [INFO](ContentDAO:145 ) execute query ...
2015-04-04 14:35:20 [INFO](ContentInfoService:38 ) update hits for content
2015-04-04 14:35:20 [INFO](ContentInfoService:89 ) check of user
2015-04-04 14:35:24 [INFO](ContentDAO:146 ) execute query ...
2015-04-04 14:35:25 [INFO](ContentDAO:123 ) execute query ...
```

Pinpoint classifies logs by requests by storing additional information(transactionId, spanId) in MDC of each request.
ex) With Pinpoint
```
2015-04-04 14:35:20 [INFO](ContentInfoCollector:76) [txId : agent^14252^17 spanId : 1224] get content name : TECH
2015-04-04 14:35:20 [INFO](ContentInfoCollector:123) [txId : agent^142533^18 spanId : 1231] get content name : OPINION
2015-04-04 14:35:20 [INFO](ContentInfoCollector:12) [txId : agent^142533^19 spanId : 1246] get content name : SPORTS
2015-04-04 14:35:20 [INFO](ContentInfoCollector:25) [txId : agent^142533^20 spanId : 1263] get content name : TECH
2015-04-04 14:35:20 [INFO](ContentInfoCollector:56) [txId : agent^142533^21 spanId : 1265] get content name : NATIONAL
2015-04-04 14:35:20 [INFO](ContentInfoCollector:34) [txId : agent^142533^22 spanId : 1278] get content name : OPINION
2015-04-04 14:35:20 [INFO](ContentInfoService:55) [txId : agent^14252^18 spanId : 1231] check authorization of user
2015-04-04 14:35:20 [INFO](ContentInfoService:14) [txId : agent^14252^17 spanId : 1224] get title of content
2015-04-04 14:35:21 [INFO](ContentDAOImpl:14) [txId : agent^14252^17 spanId : 1224] execute query ...
2015-04-04 14:35:21 [INFO](ContentDAOImpl:114) [txId : agent^142533^19 spanId : 1246] execute query ...    
2015-04-04 14:35:20 [INFO](ContentInfoService:74) [txId : agent^14252^17 spanId : 1224] get top linking for content
2015-04-04 14:35:21 [INFO](ContentDAOImpl:14) [txId : agent^142533^18 spanId : 1231] execute query ...
2015-04-04 14:35:21 [INFO](ContentDAOImpl:114) [txId : agent^142533^21 spanId : 1265] execute query ...
2015-04-04 14:35:22 [INFO](ContentDAOImpl:186) [txId : agent^142533^22 spanId : 1278] execute query ...
2015-04-04 14:35:22 [ERROR](ContentDAOImpl:158) [txId : agent^142533^18 spanId : 1231]
     com.mysql.jdbc.exceptions.jdbc4.CommunicationsException: Communications link failure
          at com.pinpoint.example.dao.ContentDAO.executequery(ContentDAOImpl.java:152)
          ...
          at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
          at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
          at com.mysql.jdbc.ConnectionImpl.<init>(ConnectionImpl.java:787)   
          ...
     Caused by: com.mysql.jdbc.exceptions.jdbc4.CommunicationsException:
     Communications link failure The last packet sent successfully to the server was 0 milliseconds ago.
     The driver has not received any packets from the server.
          ...
          at com.mysql.jdbc.ConnectionImpl.createNewIO(ConnectionImpl.java:2181)
          ... 12 more
     Caused by: java.net.ConnectException: Connection refused
          at java.net.PlainSocketImpl.socketConnect(Native Method)   
          at java.net.PlainSocketImpl.doConnect(PlainSocketImpl.java:333)
          at java.net.PlainSocketImpl.connectToAddress(PlainSocketImpl.java:195)
          at java.net.PlainSocketImpl.connect(PlainSocketImpl.java:182)
          at java.net.SocksSocketImpl.connect(SocksSocketImpl.java:432)
          at java.net.Socket.connect(Socket.java:529)
          ... 13 more
2015-04-04 14:35:22 [INFO](ContentDAO:145) [txId : agent^14252^17 spanId : 1224] execute query ...
2015-04-04 14:35:20 [INFO](ContentInfoService:38) [txId : agent^142533^19 spanId : 1246] update hits for content
2015-04-04 14:35:20 [INFO](ContentInfoService:89) [txId : agent^142533^21 spanId : 1265] check of user
2015-04-04 14:35:24 [INFO](ContentDAO:146) [txId : agent^142533^22 spanId : 1278] execute query ...
2015-04-04 14:35:25 [INFO](ContentDAO:123) [txId : agent^14252^17 spanId : 1224] execute query ...
```

The transactionId printed in the log message is the same as the transactionId in Pinpoint Web’s Transaction List view.
![per-request_feature_1.jpg](images/per-request_feature_1.jpg)

### 2. How to configure

**2-1 Pinpoint agent configuration**

To enable this feature, set the logging property corresponding to the logging library in use to true in *pinpoint.config*.
For example,

ex) pinpoint.config when using log4j
```
###########################################################
# log4j
###########################################################
profiler.log4j.logging.transactioninfo=true
```

ex) pinpoint.config when using log4j2
```
###########################################################
# log4j2 
###########################################################
profiler.log4j2.logging.transactioninfo=true

```

ex) pinpoint.config when using logback
```
###########################################################
# logback
###########################################################
profiler.logback.logging.transactioninfo=true
```

**2-2 log4j, log4j2, logback configuration**

Change the log message format to print the transactionId, and spanId saved in MDC.

ex) log4j : log4j.xml
```xml
Before
<appender name = "console" class= "org.apache.log4j.ConsoleAppender" >
     <layout class = "org.apache.log4j.EnhancedPatternLayout">
          <param name = "ConversionPattern" value= "%d{yyyy-MM-dd HH:mm:ss} [%-5p](%-30c{1}) %m%n" />
     </layout >
</appender >

After
<appender name = "console" class= "org.apache.log4j.ConsoleAppender" >
     <layout class = "org.apache.log4j.EnhancedPatternLayout">
          <param name = "ConversionPattern" value= "%d{yyyy-MM-dd HH:mm:ss} [%-5p](%-30c{1}) [TxId : %X{PtxId} , SpanId : %X{PspanId}] %m%n" />
        </layout >
</appender >
```

ex) log4j2 - log4j2.xml
```xml
Before
<appender>
     <console name="STDOUT" target="SYSTEM_OUT">
          <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} [%-5p](%-30c{1}) %m%n""/>
     </console>
<appender>

After
<appender>
     <console name="STDOUT" target="SYSTEM_OUT">
          <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} [%-5p](%-30c{1}) [TxId : %X{PtxId} , SpanId : %X{PspanId}] %m%n""/>
     </console>
<appender>
```

ex) logback : logback.xml
```xml
Before
<appender name = "STDOUT" class= "ch.qos.logback.core.ConsoleAppender" >
     <layout class = "ch.qos.logback.classic.PatternLayout">
          <Pattern >%d{HH:mm} %-5level %logger{36} - %msg%n</Pattern >
     </layout >
</appender >

After
<appender name = "STDOUT" class= "ch.qos.logback.core.ConsoleAppender" >
     <layout class = "ch.qos.logback.classic.PatternLayout">
          <Pattern >%d{HH:mm} %-5level %logger{36} - [TxId : %X{PtxId} , SpanId : %X{PspanId}] %msg%n</Pattern >
     </layout >
</appender >
```

**2-3 Checking log message**

If the per-request logging is correctly configured, the transactionId, and spanId are printed in the log file.

```  
2015-04-04 14:35:20 [INFO](ContentInfoCollector:76 ) [txId : agent^14252^17 spanId : 1224] get content name : TECH
2015-04-04 14:35:20 [INFO](ContentInfoCollector:123 ) [txId : agent^142533^18 spanId : 1231] get content name : OPINION
2015-04-04 14:35:20 [INFO](ContentInfoCollector:12) [txId : agent^142533^19 spanId : 1246] get content name : SPORTS
```
  
### 3. expose log in Pinpoint web

If you want to add links to the logs in the transaction list view, you should configure and implement the logic as below.
Pinpoint Web only adds link buttons - you should implement the logic to retrieve the log message.

If you want to expose your agent’s log messages, please follow the steps below.

**step 1**
You should implement a controller that receives transactionId, spanId, transaction_start_time as parameters and retrieve the logs yourself. 
We do not yet provide a way to retrieve the logs.

example)
```java
@RestController
public class Nelo2LogController {
  
    @RequestMapping(value = "/????")
    public String NeloLogForTransactionId(@RequestParam (value= "transactionId", required=true) String transactionId,
                                            @RequestParam(value= "spanId" , required=false) String spanId,
                                            @RequestParam(value="time" , required=true) long time ) {

          // you should implement the logic to retrieve your agent’s logs.
    }
```

**step 2**
In *pinpoint-web.properties* file, set `log.enable` to true, and `log.page.url` to the url of the controller above.
The value set in `log.button.name` will show up as the button text in the Web UI.
```properties
log.enable= true
log.page.url=XXXX.pinpoint
log.button.name= log
```

**step 3**
Pinpoint 1.5.0 or later, we improve button to decided enable/disable depending on whether or not being logged.
You should implement interceptor for using logging appender to add logic whether or not being logged. you also should create plugin for logging appender internally.
Please refer to Pinpoint Profiler Plugin Sample([Link](https://github.com/pinpoint-apm/pinpoint-plugin-sample)).
Location added logic of interceptor is method to log for data of LoggingEvent in appender class. you should review your appender class and find method.
This is interceptor example.

```
public class AppenderInterceptor implements AroundInterceptor0 {

    private final TraceContext traceContext;

    public AppenderInterceptor(TraceContext traceContext) {
        this.traceContext = traceContext;
    }

    @Override
    public void before(Object target) {
        Trace trace = traceContext.currentTraceObject();

        if (trace != null) {
            SpanRecorder recorder = trace.getSpanRecorder();
            recorder.recordLogging(LoggingInfo.LOGGED);
        }
    }

    @IgnoreMethod
    @Override
    public void after(Object target, Object result, Throwable throwable) {

    }
}
```

If those are correctly configured, the buttons are added in the transaction list view.
![per-request_feature_2.jpg](images/per-request_feature_2.jpg)

For details in how the log buttons are generated, please refer to Pinpoint Web’s BusinessTransactionController and ScatterChartController.

---------------------

# 한국어 가이드

## Per-request logging

### 1. 기능 설명

Pinpoint에서는 log message를 request 단위로 구분할 수 있도록 log message 에 추가정보를 저장해준다.

다수의 요청을 처리하는 tomcat을 사용할 경우 로그 파일을 보면 시간순으로 출력된 로그를 확인할 수 있다.
그러나 동시에 요청된 다수의 request 각각에 대한 로그를 구분 해서 볼 수 없다.
예를 들어 로그에서 exception message가 출력됐을 때 그 exception이 발생한 request의 모든 log를 확인하기 힘들다.

Pinpoint는 log message 마다 request와 연관된 정보(transactionId, spanId)를 MDC에 넣어줘서 request 단위로 log message를 구분할 수 있도록 해준다.
로그에 출력된 transactionId는 pinpoint web의 transaction List 화면에 출력된 transactionId와 일치한다.

구체적으로 예를 들어보자.
Pinpoint를 사용하지 않았을 때 exception이 발생했을 경우 로그 메시지를 살펴 보자.
요청된 다수의 request 각각을 구분하여 로그를 확인할 수가 없다.

ex) Without Pinpoint
```
2015-04-04 14:35:20 [INFO](ContentInfoCollector:76 ) get content name : TECH
2015-04-04 14:35:20 [INFO](ContentInfoCollector:123 ) get content name : OPINION
2015-04-04 14:35:20 [INFO](ContentInfoCollector:12) get content name : SPORTS
2015-04-04 14:35:20 [INFO](ContentInfoCollector:25 ) get content name : TECH
2015-04-04 14:35:20 [INFO](ContentInfoCollector:56 ) get content name : NATIONAL
2015-04-04 14:35:20 [INFO](ContentInfoCollector:34 ) get content name : OPINION
2015-04-04 14:35:20 [INFO](ContentInfoService:55 ) check authorization of user
2015-04-04 14:35:20 [INFO](ContentInfoService:14 ) get title of content
2015-04-04 14:35:21 [INFO](ContentDAOImpl:14 ) execute query ...
2015-04-04 14:35:21 [INFO](ContentDAOImpl:114 ) execute query ...
2015-04-04 14:35:20 [INFO](ContentInfoService:74 ) get top linking for content
2015-04-04 14:35:21 [INFO](ContentDAOImpl:14 ) execute query ...
2015-04-04 14:35:21 [INFO](ContentDAOImpl:114 ) execute query ...
2015-04-04 14:35:22 [INFO](ContentDAOImpl:186 ) execute query ...
2015-04-04 14:35:22 [ERROR]ContentDAOImpl:158 )
     com.mysql.jdbc.exceptions.jdbc4.CommunicationsException: Communications link failure
          at example.ContentDAO.executequery(ContentDAOImpl.java:152)
          ...
          at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
          at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
          at com.mysql.jdbc.ConnectionImpl.<init>(ConnectionImpl.java:787)
          ...
     Caused by: com.mysql.jdbc.exceptions.jdbc4.CommunicationsException:
     Communications link failure The last packet sent successfully to the server was 0 milliseconds ago.
     The driver has not received any packets from the server.
          at com.mysql.jdbc.ConnectionImpl.createNewIO(ConnectionImpl.java:2181)
          ... 12 more
     Caused by: java.net.ConnectException: Connection refused
          at java.net.PlainSocketImpl.socketConnect(Native Method)
          at java.net.PlainSocketImpl.doConnect(PlainSocketImpl.java:333)
          at java.net.PlainSocketImpl.connectToAddress(PlainSocketImpl.java:195)
          at java.net.PlainSocketImpl.connect(PlainSocketImpl.java:182)
          at java.net.SocksSocketImpl.connect(SocksSocketImpl.java:432)
          at java.net.Socket.connect(Socket.java:529)
          ...13 more
2015-04-04 14:35:22 [INFO](ContentDAO:145 ) execute query ...
2015-04-04 14:35:20 [INFO](ContentInfoService:38 ) update hits for content
2015-04-04 14:35:20 [INFO](ContentInfoService:89 ) check of user
2015-04-04 14:35:24 [INFO](ContentDAO:146 ) execute query ...
2015-04-04 14:35:25 [INFO](ContentDAO:123 ) execute query ...
```

Pinpoint는 로그 메세지 마다 request와 연관된 정보(transactionId, spanId)를 MDC에 넣어줘서 request 단위로 log message를 구분시켜 준다.

ex) With Pinpoint

```
2015-04-04 14:35:20 [INFO](ContentInfoCollector:76) [txId : agent^14252^17 spanId : 1224] get content name : TECH
2015-04-04 14:35:20 [INFO](ContentInfoCollector:123) [txId : agent^142533^18 spanId : 1231] get content name : OPINION
2015-04-04 14:35:20 [INFO](ContentInfoCollector:12) [txId : agent^142533^19 spanId : 1246] get content name : SPORTS
2015-04-04 14:35:20 [INFO](ContentInfoCollector:25) [txId : agent^142533^20 spanId : 1263] get content name : TECH
2015-04-04 14:35:20 [INFO](ContentInfoCollector:56) [txId : agent^142533^21 spanId : 1265] get content name : NATIONAL
2015-04-04 14:35:20 [INFO](ContentInfoCollector:34) [txId : agent^142533^22 spanId : 1278] get content name : OPINION
2015-04-04 14:35:20 [INFO](ContentInfoService:55) [txId : agent^14252^18 spanId : 1231] check authorization of user
2015-04-04 14:35:20 [INFO](ContentInfoService:14) [txId : agent^14252^17 spanId : 1224] get title of content
2015-04-04 14:35:21 [INFO](ContentDAOImpl:14) [txId : agent^14252^17 spanId : 1224] execute query ...
2015-04-04 14:35:21 [INFO](ContentDAOImpl:114) [txId : agent^142533^19 spanId : 1246] execute query ...
2015-04-04 14:35:20 [INFO](ContentInfoService:74) [txId : agent^14252^17 spanId : 1224] get top linking for content
2015-04-04 14:35:21 [INFO](ContentDAOImpl:14) [txId : agent^142533^18 spanId : 1231] execute query ...
2015-04-04 14:35:21 [INFO](ContentDAOImpl:114) [txId : agent^142533^21 spanId : 1265] execute query ...
2015-04-04 14:35:22 [INFO](ContentDAOImpl:186) [txId : agent^142533^22 spanId : 1278] execute query ...
2015-04-04 14:35:22 [ERROR](ContentDAOImpl:158) [txId : agent^142533^18 spanId : 1231]
     com.mysql.jdbc.exceptions.jdbc4.CommunicationsException: Communications link failure
          at com.pinpoint.example.dao.ContentDAO.executequery(ContentDAOImpl.java:152)
          ...
          at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
          at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
          at com.mysql.jdbc.ConnectionImpl.<init>(ConnectionImpl.java:787)
          ...
     Caused by: com.mysql.jdbc.exceptions.jdbc4.CommunicationsException:
     Communications link failure The last packet sent successfully to the server was 0 milliseconds ago.
     The driver has not received any packets from the server.
          ...
          at com.mysql.jdbc.ConnectionImpl.createNewIO(ConnectionImpl.java:2181)
          ... 12 more
     Caused by: java.net.ConnectException: Connection refused
          at java.net.PlainSocketImpl.socketConnect(Native Method)
          at java.net.PlainSocketImpl.doConnect(PlainSocketImpl.java:333)
          at java.net.PlainSocketImpl.connectToAddress(PlainSocketImpl.java:195)
          at java.net.PlainSocketImpl.connect(PlainSocketImpl.java:182)
          at java.net.SocksSocketImpl.connect(SocksSocketImpl.java:432)
          at java.net.Socket.connect(Socket.java:529)
          ... 13 more
2015-04-04 14:35:22 [INFO](ContentDAO:145) [txId : agent^14252^17 spanId : 1224] execute query ...
2015-04-04 14:35:20 [INFO](ContentInfoService:38) [txId : agent^142533^19 spanId : 1246] update hits for content
2015-04-04 14:35:20 [INFO](ContentInfoService:89) [txId : agent^142533^21 spanId : 1265] check of user
2015-04-04 14:35:24 [INFO](ContentDAO:146) [txId : agent^142533^22 spanId : 1278] execute query ...
2015-04-04 14:35:25 [INFO](ContentDAO:123) [txId : agent^14252^17 spanId : 1224] execute query ...
```

로그메시지에 출력된 transactionId는 Pinpoint web의 transactionlist의 transactionId와 일치한다.
![per-request_feature_1.jpg](images/per-request_feature_1.jpg)

### 2. 설정 방법

**2-1 Pinpoint agent 설정**

Pinpoint를 사용하려면 Pinpoint agent 설정파일(Pinpoint.config)의 logging 설정 값을 true로 변경해야 한다.
사용하는 logging 라이브러리에 해당하는 설정값만 true로 변경하면 된다.
아래 설정에 대한 예시가 있다.

ex) Pinpoint.config  - log4j 를 사용할 경우
```
###########################################################
# log4j
###########################################################
profiler.log4j.logging.transactioninfo=true
```

ex) Pinpoint.config  - log4j2 를 사용할 경우
```
###########################################################
# log4j2 
###########################################################
profiler.log4j2.logging.transactioninfo=true

```

ex) Pinpoint.config  - logback 를 사용할 경우
```
###########################################################
# logback
###########################################################
profiler.logback.logging.transactioninfo=true
```

**2-2 log4j, log4j2, logback 설정 파일 설정**

logging 설정 파일의 log message pattern 설정에 Pinpoint에서 MDC에 저장한 transactionId, spanId값이 출력될수 있도록 설정을 추가하자.

ex) log4j - log4j.xml
```xml
변경 전
<appender name = "console" class= "org.apache.log4j.ConsoleAppender" >
     <layout class = "org.apache.log4j.EnhancedPatternLayout">
          <param name = "ConversionPattern" value= "%d{yyyy-MM-dd HH:mm:ss} [%-5p](%-30c{1}) %m%n" />
     </layout >
</appender >

변경 후
<appender name = "console" class= "org.apache.log4j.ConsoleAppender" >
     <layout class = "org.apache.log4j.EnhancedPatternLayout">
          <param name = "ConversionPattern" value= "%d{yyyy-MM-dd HH:mm:ss} [%-5p](%-30c{1}) [TxId : %X{PtxId} , SpanId : %X{PspanId}] %m%n" />
        </layout >
</appender >
```

ex) log4j2 - log4j2.xml
```xml
변경 전
<appender>
     <console name="STDOUT" target="SYSTEM_OUT">
          <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} [%-5p](%-30c{1}) %m%n""/>
     </console>
<appender>

변경 후
<appender>
     <console name="STDOUT" target="SYSTEM_OUT">
          <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} [%-5p](%-30c{1}) [TxId : %X{PtxId} , SpanId : %X{PspanId}] %m%n""/>
     </console>
<appender>
```

ex) logback - logback.xml
```xml
변경 전
<appender name = "STDOUT" class= "ch.qos.logback.core.ConsoleAppender" >
     <layout class = "ch.qos.logback.classic.PatternLayout">
          <Pattern >%d{HH:mm} %-5level %logger{36} - %msg%n</Pattern >
     </layout >
</appender >

변경 후
<appender name = "STDOUT" class= "ch.qos.logback.core.ConsoleAppender" >
     <layout class = "ch.qos.logback.classic.PatternLayout">
          <Pattern >%d{HH:mm} %-5level %logger{36} - [TxId : %X{PtxId} , SpanId : %X{PspanId}] %msg%n</Pattern >
     </layout >
</appender >
```

**2-3 로그 출력 확인**

Pinpoint agent가 적용된 서비스를 동작하여 log message에 아래와 같이 transactionId, spanId 정보가 출력되는것을 확인하면 된다.

```
2015-04-04 14:35:20 [INFO](ContentInfoCollector:76 ) [txId : agent^14252^17 spanId : 1224] get content name : TECH
2015-04-04 14:35:20 [INFO](ContentInfoCollector:123 ) [txId : agent^142533^18 spanId : 1231] get content name : OPINION
2015-04-04 14:35:20 [INFO](ContentInfoCollector:12) [txId : agent^142533^19 spanId : 1246] get content name : SPORTS
```

### 3. Pinpoint web에서 로그 확인
Pinpoint web의 transaction list 화면에서 log를 출력하는 링크를 제공하고 싶다면 아래와 같이 설정 및 구현을 추가하면 된다.
Pinpoint web에서는 버튼 을 추가해주기만 하고 로그를 가져오는 로직은 직접 구현을 해야한다.


로그 메시지를 Pinpoint web에서 보여주기 위해서는 아래와 같이 2가지 step을 따라야 한다.

**step 1**
transactionId와 spanId, transaction 시작 시간을 파라미터로 받아서 로그 메시지를 가져오는 controller을 구현해야한다.

example)
```java
@RestController
public class Nelo2LogController {

    @RequestMapping(value = "/XXXX")
    public String NeloLogForTransactionId(@RequestParam (value= "transactionId", required=true) String transactionId,
                                            @RequestParam(value= "spanId" , required=false) String spanId,
                                            @RequestParam(value="time" , required=true) long time ) {

          // you should implement the logic to retrieve your agent’s logs.
    }
```


**step 2**
Pinpoint-web.properties 파일에서 버튼을 추가해주는 기능을 활성화 하기 위해서 log.enable의 값을 true로 설정하고
위에서 구현한 controller의 url과 button의 이름을 추가해주자.

```properties
log.enable=true
log.page.url=XXXX.Pinpoint
log.button.name=log
```


**step 3**
pinpoint 1.5 이후 버전부터 log 기록 여부에 따라 log 버튼의 활성화가 결정되도록 개선 됐기 때문에
당신이 사용하는 logging appender의 로깅 메소드에 logging 여부를 저장하는 interceptor를 추가하는 플러그인을 개발해야 한다.
플러그인 개발 방법은 다음 링크를 참고하면 된다([Link](https://github.com/pinpoint-apm/pinpoint-plugin-sample)). interceptor 로직이 추가돼야 하는 위치는 appender class 내에 LoggingEvent 객체의 데이터를 이용하여 로깅을 하는 메소드다.
아래는 interceptor 예제이다.
```
public class AppenderInterceptor implements AroundInterceptor0 {

    private final TraceContext traceContext;

    public AppenderInterceptor(TraceContext traceContext) {
        this.traceContext = traceContext;
    }

    @Override
    public void before(Object target) {
        Trace trace = traceContext.currentTraceObject();

        if (trace != null) {
            SpanRecorder recorder = trace.getSpanRecorder();
            recorder.recordLogging(LoggingInfo.LOGGED);
        }
    }

    @IgnoreMethod
    @Override
    public void after(Object target, Object result, Throwable throwable) {

    }
}
```


위와 같이 설정 및 구현을 추가하고 pinpoint web을 동작시키면 아래와 같이 버튼이 추가 된다.
![per-request_feature_2.jpg](images/per-request_feature_2.jpg)
로그 버튼을 생성해주는 과정을 보시려면, Pinpoint Web의 BusinessTransactionController 와 ScatterChartController class를 참고하세요.
