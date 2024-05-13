/**
 * <em>LogFMT</em> is a popular format for processing log files, especially in the Kubernetes and
 * Grafana/Loki environment. By default, SLF4J does not provide support for generating log outputs
 * in that format. This extension tries to close this shortcoming.
 *
 * <p>In order to use this extension the layout needs to be specified in the i.e.
 * <em>logback.xml</em> configuration like this:
 * <pre><code>
 * &lt;appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
 *   &lt;withJansi>false&lt;/withJansi>
 *   &lt;encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
 *     &lt;layout class="com.ecosio.logfmt.LogFmtLayout" />
 *   &lt;/encoder>
 * &lt;/appender>
 * </code></pre>
 * or with some configuration like that:
 * <pre><code>
 * &lt;appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
 *   &lt;withJansi>false&lt;/withJansi>
 *   &lt;encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
 *     &lt;layout class="com.ecosio.logfmt.LogFmtLayout" />
 *       &lt;prefix>dev&lt;/prefix>
 *       &lt;appName>test-app&lt;/appName>
 *       &lt;timeFormat>yyyy-MM-dd HH:mm:ss.SSSZ&lt;/timeFormat>
 *       &lt;fields>time,level,msg,thread,package,module,mdc,custom,error&lt;/fields>
 *       &lt;maskPasswords>pw,userKey&lt;/maskPasswords>
 *   &lt;/encoder>
 * &lt;/appender>
 * </code></pre>
 *
 * <p>Above configuration will initialize the {@link com.ecosio.logfmt.LogFmtLayout} layout, which
 * is responsible for collecting the various key/value pairs and log them to a target location. In
 * the above sample the log was simply printed to the standard output. Of course, this could also be
 * written to a log file or sent to a remote location.
 *
 * <p>Above-mentioned <code>LogFmtLayout</code> is the main component responsible for generating
 * <em>LogFMT</em> conform output. It will basically take every configured key and its assigned
 * value and produce a log line such as:
 * <pre><code>
 *   key1=value1 key2=value2 ...
 * </code></pre>
 * It will come with a predefined list of keys which order to appear in the logs can be partially
 * modified. The column below lists the default keys <code>LogFmtLayout</code> will configure and
 * add to the output:
 *
 * <p><table border="1">
 *   <tr>
 *     <td>Key</td><td>Position modifiable</td><td>Description</td>
 *   </tr>
 *   <tr>
 *     <td>prefix</td><td>false</td><td>A prefix value that is added to each log line produced. This
 *     is a constant value defined in the configuration of the LogFMT layout and may contain i.e.
 *     information on the environment, like <em>dev</em>, <em>test</em> or <em>prod</em>. This is an
 *     optional value and may be omitted if not needed.</td>
 *   </tr>
 *   <tr>
 *     <td>app</td><td>false</td><td>A field for the name of the application. This value is defined
 *     in the configuration of the LogFMT layout and may be omitted if not needed.</td>
 *   </tr>
 *   <tr>
 *     <td>time</td><td>true</td><td>Time value in <code>yyyy-MM-dd'T'HH:mm:ss</code> format.
 *     The desired output format can be configured via the
 *     <code>&lt;timeFormat>...&lt;/timeFormat></code> configuration option.</td>
 *   </tr>
 *   <tr>
 *     <td>level</td><td>true</td><td>The log level of the current line produced.</td>
 *   </tr>
 *   <tr>
 *     <td>thread</td><td>true</td><td>The name of the thread the log statement originates from.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>package</td><td>true</td><td>The name of the package the log statement originates from.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>module</td><td>true</td><td>The simplified class name the log statement originates from.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>msg</td><td>true</td><td>The actual message to log.</td>
 *   </tr>
 *   <tr>
 *     <td>mdc</td><td>true</td><td>Will output any keys and its values found within the current
 *     message digest context (MDC) as key=value pairs. Note that if a MDC key/value is found with
 *     the same key name as other predefined fields, the key entry will occur multiple times within
 *     the log line.</td>
 *   </tr>
 *   <tr>
 *     <td>custom</td><td>true</td><td>Any custom defined key/value entries to add to the log
 *     output. Each of the defined key/value entries will appear as <code>key=value</code> output in
 *     the log. If a key uses the same name as a previous field, the key will occur multiple time
 *     within the log line.</td>
 *   </tr>
 *   <tr>
 *     <td>error</td><td>true</td><td>The actual error or stacktrace that produced this log line.
 *     Note that the logged exception will contain <code>\n</code> characters instead of actual new
 *     lines.</td>
 *   </tr>
 * </table>
 *
 * <p>In order to configure the position of keys within a log line, <code>LogFmtLayout</code> allows
 * to add <code>&lt;fields>...&lt;/fields></code> list where the above-mentioned names can be
 * configured in the desired order. Also, the desired time format that should appear within the logs
 * can be configured via <code>&lt;timeFormat>...&lt;/timeFormat></code> where the actual format
 * should correspond to the one defined for {@link java.time.format.DateTimeFormatter}. Fields that
 * contain passwords can be masked by providing comma separated list of the actual key names whose
 * values need to be masked within a <code>&lt;maskPasswords>...&lt;/maskPasswords></code> list.
 *
 * <p>While values contained in the message digest context (MDC) are automatically picked up and
 * exposed as key/value entries per MDC property, where the property name servers as key name and
 * the value of that MDC property as value for that key, if <em>mdc</em> is part of the
 * <em>fields</em> list, support for defining custom key/value entries is supported by adding
 * <em>custom</em> to the <em>fields</em> list and specifying the keys and values to add to the log
 * on a {@link com.ecosio.logfmt.LogFmtMarker} object.
 *
 * <p><code>LogFmtMarker</code> is a marker class that can be used similar to traditional markers. A
 * marker is often used to state that the content of the log statement at hand contains confidential
 * information and thus special treatment should be applied on that log statement to redact
 * confidential data from that log statement. {@link com.ecosio.logfmt.LogFmtMarker#with} allows to
 * define custom key/values on the marker alongside the name of the logger, like
 * <em>confidential</em> as mentioned before. Markers can be chained by using the {@link
 * org.slf4j.Marker#add(org.slf4j.Marker)} method. This implementation will respect chained markers
 * and process them in the order they are chained, with the outer one being processed first. In
 * order to pass along a marker object with a log statement use the following approach, which
 * demonstrates how markers can get nested:
 * <pre><code>
 * Marker root = LogFmtMarker.with("nodeName", nodeName).and("containerName", containerName);
 * Marker marker = LogFmtMarker.with("trace-id", traceId);
 * marker.add(root);
 * log.info(marker, "Some log message with argument {}", "someParam");
 * </code></pre>
 *
 * <p>Note that if a marker is added with the name <code>CONFIDENTIAL</code> this implementation
 * will mask any <em>Basic Auth</em> string, like <code>Basic dXNlcjpwYXNzd29yZAo=</code>, or
 * <em>URL Auth</em>, like <code>https://user:pw@example.org</code>), by default. Such confidential
 * markers can be created either via
 * <pre><code>
 * Marker confidential = MarkerFactory.getMarker("CONFIDENTIAL");
 * log.trace(confidential, "Invoking URL: {}", urlContainingUsernameAndPassword);
 * </code></pre>
 * or via
 * <pre><code>
 * Marker confidential = LogFmtMarker.with("CONFIDENTIAL", key1, val1).and(key2, val2);
 * log.trace(confidential, "Invoking URL: {}", urlContainingUsernameAndPassword);
 * </code></pre>
 * In both cases the URL containing a username and password will be masked replacing the username
 * and password with <code>***</code> effectively.
 *
 * <p>In case the message to log or the exception being added to the log needs to be customized
 * before adding it to the log, {@link com.ecosio.logfmt.LogFmtMarker#withCustomized}
 * can be used. The passed in <code>BiFunction</code> will receive the original message or error as
 * first argument and a map of key/value entries as second argument. The second parameter allows to
 * add key/values dynamically to the marker and thus to the log statement produced. The return value
 * of the function is the message that should appear within the log. Note that if marker chaining is
 * used and multiple markers use this customization approach, the output of the first marker will be
 * the input of the 2nd marker and so forth.
 *
 * <p>The example below shows how the history information contained in a log can be stripped from
 * the actual message to log and instead expose that stripped information as own <em>history</em>
 * key within the generated log line:
 * <pre><code>
 * Marker marker = LogFmtMarker.withCustomized(ApplyCallbackFor.MESSAGE,
 *       (msg, keyValues) -> {
 *         if (msg.startsWith("Failed delivery for")) {
 *           String separator = "--------------";
 *           int idx = msg.lastIndexOf(separator) + separator.length();
 *           String history = msg.substring(0, idx);
 *           String message = msg.substring(idx + 1);
 *
 *           keyValues.add(new LogFmtMarker.KeyValue("history", history));
 *           return message;
 *         }
 *         return msg;
 *       });
 * log.debug(marker, "history part--------------Part that should remain in the message");
 * </code></pre>
 * The logged statement after this customization will show the part after the
 * <code>--------------</code> separator within the <code>msg</code> field while the part before
 * that separator will be exposed as <code>history</code> key in that log statement.
 *
 * @author Roman Vottner
 */
package com.ecosio.logfmt;