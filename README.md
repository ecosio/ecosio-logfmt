# ecosio-logfmt

This project is originally based on [BatchLabs escalog](https://github.com/BatchLabs/escalog) 
*logfmt* logback layout, which generates a [logfmt](https://brandur.org/logfmt) compliant log 
message output. Since then though a bit of customization went into this project. I.e. the custom 
*LogFmt* logger and its builder got removed as it didn't serve any purpose for our use case.

Instead of sharing an internal marker instance among all *LogFmtMarker* objects but keeping the 
properties on the respective marker objects themselves, we modified *LogFmtMarker* to allow for 
properly implementing the methods defined by the [Marker interface](
https://www.slf4j.org/api/org/slf4j/Marker.html). This allows to define root or base markers that 
can be added to child markers and *LogFmtLayout* will pick up the defined custom properties from 
these markers and add those to the log message. 

## Usage

In order to enable *logfmt* log message formatting make sure the *slf4j-api*, 
*logback-classic* and *ecosio-logfmt* are on the classpath. 

### Maven

In Maven the above-mentioned dependencies can be defined and imported like this:

```xml
  <dependencies>
    ...
    <!-- https://mvnrepository.com/artifact/org.slf4j/slf4j-api -->
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>2.0.12</version>
    </dependency>
    <!-- https://mvnrepository.com/artifact/ch.qos.logback/logback-classic -->
    <dependency>
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-classic</artifactId>
      <version>1.5.3</version>
    </dependency>
    <dependency>
      <groupId>com.ecosio</groupId>
      <artifactId>ecosio-logfmt</artifactId>
      <version>1.0.3</version>
    </dependency>
    ...
  </dependencies>
```

Once the dependencies are in place *logback* needs to be configured.

### Logback XML

For configuring logback through its *logback.xml* a new [LayoutWrappingEncoder](
https://logback.qos.ch/manual/encoders.html#LayoutWrappingEncoder) needs to be defined which 
specifies to format messages via the **LogFmtLayout** formatter.

A simple *logback.xml* may look like this:

```xml
<configuration>

  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <withJansi>false</withJansi>
    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
      <layout class="com.ecosio.logfmt.LogFmtLayout" />
    </encoder>
  </appender>

  <logger name="com.ecosio.TestApp" level="debug" additivity="false">
    <appender-ref ref="STDOUT"/>
  </logger>

  <root level="INFO">
    <appender-ref ref="STDOUT"/>
  </root>

</configuration>
```

An exemplary output for such a default *logfmt*  layout may look like the sample output below:

```console
time="2023-06-12T18:55:10" level=info thread=main package=com.ecosio module=TestApp msg="This is a simple log line"
time="2023-06-12T18:55:10" level=info thread=main package=com.ecosio module=TestApp msg="This is a simple log line with an added value 'param'"
time="2023-06-12T18:55:10" level=info thread=main package=com.ecosio module=TestApp msg="This is a log with attached MDC context" userKey=abcdef56789
time="2023-06-12T18:55:10" level=info thread=main package=com.ecosio module=TestApp msg="This is a log with attached MDC context and a value 'param1'" mdc=value2
time="2023-06-12T18:55:10" level=warn thread=main package=com.ecosio module=TestApp msg="Some problem encountered" error="java.lang.Exception: null\n\tat com.ecosio.TestApp.logError(TestApp.java:94)\n\tat com.ecosio.TestApp.standardLogger(TestApp.java:34)\n\tat com.ecosio.TestApp.<init>(TestApp.java:20)\n\tat com.ecosio.TestApp.main(TestApp.java:16)\n"
```

The configuration of the *LogFmtLayout* can be customized to include the name of the application 
producing the log by default or to change the order and which properties to log.

By changing the logback configuration to the one below we redefined both the order of the fields 
as well as which properties to show besides redefining the time format and adding two additional 
predefined properties, *prefix* and *app* which are always added at the start of the message, 
when enabled.

```xml
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <withJansi>false</withJansi>
    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
      <layout class="com.ecosio.logfmt.LogFmtLayout">
        <prefix>dev</prefix>
        <appName>test-app</appName>
        <timeFormat>yyyy-MM-dd HH:mm:ss.SSSZ</timeFormat>
        <fields>time,level,msg,thread,package,module,mdc,custom,error</fields>
        <maskPasswords>pw,userKey</maskPasswords>
      </layout>
    </encoder>
  </appender>
```

This will generate a log output to the one below:

```console
prefix=dev app=test-app time="2023-06-12 18:55:10.324+0200" level=info msg="This is a simple log line" thread=main package=com.ecosio module=TestApp
prefix=dev app=test-app time="2023-06-12 18:55:10.327+0200" level=info msg="This is a simple log line with an added value 'param'" thread=main package=com.ecosio module=TestApp
prefix=dev app=test-app time="2023-06-12 18:55:10.329+0200" level=info msg="This is a log with attached MDC context" thread=main package=com.ecosio module=TestApp userKey="abc***789"
prefix=dev app=test-app time="2023-06-12 18:55:10.329+0200" level=info msg="This is a log with attached MDC context and a value 'param1'" thread=main package=com.ecosio module=TestApp mdc=value2
prefix=dev app=test-app time="2023-06-12 18:55:10.329+0200" level=warn msg="Some problem encountered" thread=main package=com.ecosio module=TestApp error="java.lang.Exception: null\n\tat com.ecosio.TestApp.logError(TestApp.java:94)\n\tat com.ecosio.TestApp.standardLogger(TestApp.java:34)\n\tat com.ecosio.TestApp.<init>(TestApp.java:20)\n\tat com.ecosio.TestApp.main(TestApp.java:16)\n"
```

The default order for fields is:

```
[prefix],[app],time,level,thread,package,module,msg,mdc,custom,error
```

Where the position of the *prefix* and *app* properties can't be redefined as they are 
configured via their corresponding layout configuration elements, *&lt;prefix&gt;foo&lt;
/prefix&gt;* and *&lt;appName&gt;bar&lt;/appName&gt;* accordingly.

#### MDC

*LogFmtLayout* supports adding message diagnostic context (MDC) information to the message if 
either the default set of property fields is used or the redefined `<fields>...</fields>` list 
contains **mdc**.

If in Java a property is put into the MDC context and a log message is written the 
*LogFmtLayout*  is picking up the keys and attached values from the MDC context and add each 
key-value pair in the form of `key=value` to the log message. 

In the case below *someKey*  is added to the MDC context with a value of *someValue* 

```java
MDC.put("someKey", "someValue");
log.info("Test message");
MDC.clear();
```

*LogFmtLayout*  will pick up the MDC key and value and add it to the log message as can be seen 
in the sample output below:

```console
time="2023-06-12T18:55:10" level=info thread=main package=com.ecosio module=TestApp msg="Test message" someKey=someValue
```

As can be seen `someKey=someValue` was added to the log message automatically. If multiple 
properties are set on the MDC context each of these properties will be rendered in the 
`key=value` syntax.

#### Custom properties

In case MDC can't or shouldn't be used the *LogFmtLayout* supports adding custom properties 
through **LogFmtMarker**.

In Java this can be done this way:

```java
Marker root = LogFmtMarker.with("nodeName", nodeName).and("containerName", containerName);
Marker marker = LogFmtMarker.with("customKey", "customVal").and("otherKey", "otherVal");
marker.add(root);
log.info(marker, "Log line with a LogFmt marker and a value '{}'", "someParam");
```

which will generate the following output:

```console
time="2023-06-12T18:55:10" level=info thread=main package=com.ecosio module=TestApp msg="Log line with a LogFmt marker and a value 'someParam'" nodeName=node123 containerName=container1234 customKey=customVal otherKey=otherVal
```

Mixing MDC and custom properties is supported. If the same key name is used for an MDC entry and 
a custom property then first the MDC value is added to the log message and later on the property 
from the marker.

### Masking passwords

*LogFmtLayout* supports masking properties that represent passwords by specifying the property 
in a comma separated list via the `<maskPasswords>.../<maskPasswords>` configuration element.

```xml
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <withJansi>false</withJansi>
    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
      <layout class="com.ecosio.logfmt.LogFmtLayout">
        <maskPasswords>pw,userKey</maskPasswords>
      </layout>
    </encoder>
  </appender>
```

In this sample case both the values of properties *pw* and *userKey* will be masked with `***`.

For confidential data within a log message masking such information is possible by adding a 
marker with the name `CONFIDENTIAL` to the log message. Currently *LogFmtLayout* is able to mask 
passwords within URIs if the password is part of the form *scheme://user:password@host* as well 
as masking basic authorization headers within raw HTTP requests.

```java
Marker confidential = MarkerFactory.getMarker("CONFIDENTIAL");
log.debug(confidential, "Using URL '{}' to request data", "https://user1:secretPassword@example.com/api/someService");
log.trace(confidential, "Sending raw HTTP request: {}", httpRequestContainingBasicAuthAuthorizationHeader);
```

This marker can of course be chained with *LogFmtMarker* to add additional properties. Special 
attention needs to be put in case a *LogFmtMarker* is added to a marker created by 
*MarkerFactory*, i.e.

```java
Marker logfmt = LogFmtMarker.with("userKey", "abcd1234");
Marker confidential = MarkerFactory.getMarker("CONFIDENTIAL");
confidential.add(logfmt);
log.debug(confidential, "Using URL '{}' to request data", "https://user1:secretPassword@example.com/api/someService");
log.trace(confidential, "Sending raw HTTP request: {}", httpRequestContainingBasicAuthAuthorizationHeader);
```

as in this case retrievals of the *confidential* marker through the *MarkerFactory*  will result 
in a marker that already has the *logfmt* marker attached to it. If this is not wanted you need 
to manually remove the marker like this:

```java
confidential.remove(logfmt);
```

### Customizing `msg` and/or `error` fields

Since 1.0.3 it is possible to customize the value logged for `msg` and/or `error` fields in the
generated log line by providing a callback hook that is applied before generating the log itself.

The hook will receive the current value of that field as well as a list of currently configured
key-value entries in the respective marker and expects a string value being returned that is
then used as the new value for the field the callback was created for. The reference of the
key-value list can be used to add new key-value pairs to the marker, i.e. as part of a
filtering of the actual message where parts of the original message should be exposed via a
custom field.

In the sample below, the first part of the original message is filtered from that message but
exposed as a new `history` field.

```java
Marker marker = LogFmtMarker.withCustomized(ApplyCallbackFor.MESSAGE,
        (msg, keyValues) -> {
          if (msg.startsWith("Failed delivery for")) {
            String separator = "--------------";
            int idx = msg.lastIndexOf(separator) + separator.length();
            String history = msg.substring(0, idx);
            String message = msg.substring(idx + 1);

            keyValues.add(new LogFmtMarker.KeyValue("history", history));
            return message;
          }
          return msg;
        });
log.debug(marker, "history part--------------Part that should remain in the message");
```

The above callback hook will generate an output like the one below. Note that the `msg` field
now does not contain the "history part" which though now appears as its own `history` field in
the generated log line.

```console
time="2023-06-12T18:55:10" level=debug thread=main package=com.ecosio module=TestApp msg="Part that should remain in the message" history="history part--------------" someKey=someValue
```

If multiple `LogFmtMarker`s are chained, the output of the first marker will be the input to the
next marker and so forth. The generated output therefore is a result of all used markers if they
have a preprocessing hook defined for a particular field. If the line passed to the
customization hook does not meet certain user-defined expectations the original input message
should be returned by that hook.
