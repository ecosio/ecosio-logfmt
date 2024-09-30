package com.ecosio.logfmt;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.LayoutBase;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Marker;

/**
 * Logback Layout that format logs with logfmt format (ie. {@code level="debug" ... key1="value1"
 * key2="value2" ...})
 *
 * @author Guillaume PERRUDIN
 * @author Roman Vottner
 */
public class LogFmtLayout extends LayoutBase<ILoggingEvent> {

  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

  /**
   * Native keys that are automatically added by the Layout.
   *
   * <p>Cannot be used with Markers and MDC!
   */
  enum NativeKey {
    TIME("time"),
    LEVEL("level"),
    MESSAGE("msg"),
    APP("app"),
    THREAD("thread"),
    PACKAGE("package"),
    MODULE("module"),
    ERROR("error");

    final String text;

    NativeKey(final String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return text;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isNativeKey(String key) {
      NativeKey[] keys = values();
      for (NativeKey nativeKey : keys) {
        if (nativeKey.text.equals(key)) {
          return true;
        }
      }

      return false;
    }
  }

  private String prefix = null;
  private String appName = null;
  private String timeFormat;
  private final Map<String, KeyValueAppender> appenders = new HashMap<>();
  private final List<KeyValueAppender> defaultAppenders;
  private List<KeyValueAppender> customAppenders;
  private List<String> maskPasswords;
  private final Pattern basicAuthPattern =
          Pattern.compile("([Bb])asic ([a-zA-Z0-9+/=]{3})[a-zA-Z0-9+/=]*([a-zA-Z0-9+/=]{3})");
  private final Pattern urlAuthorizationPattern = Pattern.compile("//(.*?):.*?@");
  private final ThreadLocal<SimpleDateFormat> simpleDateFormat =
          ThreadLocal.withInitial(
                  () -> new SimpleDateFormat(timeFormat != null ? timeFormat : DATE_FORMAT)
          );

  /**
   * Instantiates a new SLF4J log layout class that will format log messages in the
   * <em>logfmt</em> format.
   *
   * <p>On initializing this layout will first register supported appender and then set up a list
   * of default appender which will be processed one by one for each log message.
   */
  public LogFmtLayout() {
    appenders.put(NativeKey.TIME.toString(), this::timeAppender);
    appenders.put(NativeKey.LEVEL.toString(), this::levelAppender);
    appenders.put(NativeKey.MESSAGE.toString(), this::msgAppender);
    appenders.put(NativeKey.THREAD.toString(), this::threadAppender);
    appenders.put("package", this::packageAppender);
    appenders.put("module", this::moduleAppender);
    appenders.put("mdc", this::mdcAppender);
    appenders.put("custom", this::customFieldsAppender);
    appenders.put(NativeKey.ERROR.toString(), this::errorAppender);

    this.defaultAppenders = List.of(
            appenders.get(NativeKey.TIME.toString()),
            appenders.get(NativeKey.LEVEL.toString()),
            appenders.get(NativeKey.THREAD.toString()),
            appenders.get("package"),
            appenders.get("module"),
            appenders.get(NativeKey.MESSAGE.toString()),
            appenders.get("mdc"),
            appenders.get("custom"),
            appenders.get(NativeKey.ERROR.toString())
    );
  }

  /**
   * This method will be called by logback based on the respective XML configuration of this
   * layout. If the XML contains a definition like
   *
   * <pre><code>
   * &lt;appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender"&gt;
   *   &lt;withJansi>false&lt;/withJansi&gt;
   *   &lt;encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder"&gt;
   *     &lt;layout class="com.ecosio.logfmt.LogFmtLayout"&gt;
   *       &lt;prefix&gt;dev&lt;/prefix&gt;
   *     &lt;/layout&gt;
   *   &lt;/encoder&gt;
   * &lt;/appender&gt;</code></pre>
   * a {@code prefix=dev} key-value property will be added to the start of each log message.
   *
   * @param prefix The name of the prefix property to add to each log message. If no prefix
   *               configuration property is present no prefix will be added to the log message
   */
  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  /**
   * This method will be called by logback based on the respective XML configuration of this
   * layout. If the XML contains a definition like
   *
   * <pre><code>
   * &lt;appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender"&gt;
   *   &lt;withJansi>false&lt;/withJansi&gt;
   *   &lt;encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder"&gt;
   *     &lt;layout class="com.ecosio.logfmt.LogFmtLayout"&gt;
   *       &lt;appName&gt;test-app&lt;/appName&gt;
   *     &lt;/layout&gt;
   *   &lt;/encoder&gt;
   * &lt;/appender&gt;</code></pre>
   * a {@code app=test-app} key-value property will be added to the start of each log message
   * only preceded by an eventually defined {@link #setPrefix(String) prefix} value.
   *
   * @param appName The name of the application to add to each log message. If none was specified
   *               in the configuration XML then no app name will be added to the log message
   */
  public void setAppName(String appName) {
    this.appName = appName;
  }

  /**
   * This method will be called by logback based on the respective XML configuration of this
   * layout. If the XML contains a definition like
   *
   * <pre><code>
   * &lt;appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender"&gt;
   *   &lt;withJansi>false&lt;/withJansi&gt;
   *   &lt;encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder"&gt;
   *     &lt;layout class="com.ecosio.logfmt.LogFmtLayout"&gt;
   *       &lt;timeFormat&gt;yyyy-MM-dd HH:mm:ss.SSSZ&lt;/timeFormat&gt;
   *     &lt;/layout&gt;
   *   &lt;/encoder&gt;
   * &lt;/appender&gt;</code></pre>
   * the default date-time format ({@code yyyy-MM-dd'T'HH:mm:ss}) is replaced with the one
   * provided in the XML configuration.
   *
   * @param timeFormat The new date and time format to use for representing the time value
   */
  public void setTimeFormat(String timeFormat) {
    try {
      new SimpleDateFormat(timeFormat);
      this.timeFormat = timeFormat;
    } catch (Exception e) {
      System.err.println("Could not update time format. Falling back to default one. Reason "
              + "given: " + e.getMessage());
    }
  }

  /**
   * This method will be called by logback based on the respective XML configuration of this
   * layout. If the XML contains a definition like
   *
   * <pre><code>
   * &lt;appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender"&gt;
   *   &lt;withJansi>false&lt;/withJansi&gt;
   *   &lt;encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder"&gt;
   *     &lt;layout class="com.ecosio.logfmt.LogFmtLayout"&gt;
   *       &lt;fields&gt;time,level,msg,thread,package,module,mdc,custom,error&lt;/fields&gt;
   *     &lt;/layout&gt;
   *   &lt;/encoder&gt;
   * &lt;/appender&gt;</code></pre>
   * <em>LogFmtLayout</em> is configured to only show the respective fields in the logs in the
   * specified order. In order to exclude i.e. the package name from any log messages simply
   * remove that entry from the fields' element. Any unknown entries to the list will be ignored.
   *
   * @param fields A comma separated list of value appender to include in the log message. Note
   *               that the order of the elements within the list is important. An entry
   *               appearing in the list before another appender will result in that appender
   *               being executed before the other appender
   */
  public void setFields(String fields) {
    customAppenders = new ArrayList<>();
    for (String field : fields.split(",")) {
      KeyValueAppender appender = appenders.get(field.trim());
      if (appender != null) {
        customAppenders.add(appender);
      }
    }
  }

  /**
   * This method will be called by logback based on the respective XML configuration of this
   * layout. If the XML contains a definition like
   *
   * <pre><code>
   * &lt;appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender"&gt;
   *   &lt;withJansi>false&lt;/withJansi&gt;
   *   &lt;encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder"&gt;
   *     &lt;layout class="com.ecosio.logfmt.LogFmtLayout"&gt;
   *       &lt;maskPasswords&gt;pw,userKey&lt;/maskPasswords&gt;
   *     &lt;/layout&gt;
   *   &lt;/encoder&gt;
   * &lt;/appender&gt;</code></pre>
   * <em>LogFmtLayout</em> is configured to mask values of the listed properties.
   *
   * @param propsToMask A comma separated list of properties that should be masked when they
   *                    appear in a log statement. These properties can either be part of MDC
   *                    or provided via a {@link LogFmtMarker}
   */
  public void setMaskPasswords(String propsToMask) {
    maskPasswords = new ArrayList<>();
    Collections.addAll(maskPasswords, propsToMask.split(","));
  }

  @Override
  public String doLayout(ILoggingEvent event) {
    StringBuilder sb = new StringBuilder();
    if (prefix != null) {
      sb.append("prefix=").append(prefix).append(' ');
    }
    if (appName != null) {
      appendKeyValueAndEscape(sb, NativeKey.APP.toString(), appName);
    }

    for (KeyValueAppender keyValueAppender : customAppenders != null
            ? customAppenders
            : defaultAppenders) {
      keyValueAppender.append(sb, event);
    }
    sb.setCharAt(sb.length() - 1, '\n');

    return sb.toString();
  }

  private void levelAppender(StringBuilder sb, ILoggingEvent event) {
    appendKeyValueAndEscape(sb, NativeKey.LEVEL.toString(),
            formatLogLevel(event.getLevel()));
  }

  private void timeAppender(StringBuilder sb, ILoggingEvent event) {
    appendKeyValueAndEscape(sb, NativeKey.TIME.toString(),
            simpleDateFormat.get().format(new Date(event.getTimeStamp())));
  }

  private void threadAppender(StringBuilder sb, ILoggingEvent event) {
    appendKeyValueAndEscape(sb, NativeKey.THREAD.toString(), event.getThreadName());
  }

  private void msgAppender(StringBuilder sb, ILoggingEvent event) {
    String msg = event.getFormattedMessage();
    List<Marker> markers = event.getMarkerList();
    if (markers != null) {
      Optional<Marker> confidential = markers.stream()
              .filter(m -> "CONFIDENTIAL".equals(m.getName()) || m.contains("CONFIDENTIAL"))
              .findFirst();
      if (confidential.isPresent()) {
        // Authorization: Basic dXNlcjpwYXNzd29yZA==
        Matcher basicAuthMatcher = basicAuthPattern.matcher(msg);
        // sftp://user:password@host:port/path/to/resource
        Matcher urlAuthMatcher = urlAuthorizationPattern.matcher(msg);

        if (basicAuthMatcher.find()) {
          msg = basicAuthMatcher.replaceAll("$1asic $2***$3");
        }
        if (urlAuthMatcher.find()) {
          msg = urlAuthMatcher.replaceAll("//$1:***@");
        }
      }

      msg = handleCustomCallbacks(markers, msg, ApplyCallbackFor.MESSAGE);
    }
    appendKeyValueAndEscape(sb, NativeKey.MESSAGE.toString(), msg);

    appendCustomCallbackKeysIfNotPresentYet(sb, markers, NativeKey.MESSAGE.toString());
  }

  private String handleCustomCallbacks(List<Marker> markers, String message,
                                       ApplyCallbackFor applyFor) {
    String msg = message;
    for (Marker marker : markers) {
      if (marker instanceof LogFmtMarker logFmtMarker) {
        if (logFmtMarker.hasCallbacks()) {
          msg = logFmtMarker.applyCallbackFor(msg, applyFor);
        }
      }
    }

    return msg;
  }

  private void mdcAppender(StringBuilder sb, ILoggingEvent event) {
    Map<String, String> mdc = event.getMDCPropertyMap();
    if (mdc != null) {
      mdc.forEach((k, v) -> {
        if (!NativeKey.isNativeKey(k)) {
          appendKeyValueAndEscape(sb, k, v);
        }
      });
    }
  }

  private void customFieldsAppender(StringBuilder sb, ILoggingEvent event) {
    List<Marker> markers = event.getMarkerList();
    if (markers != null) {
      for (Marker marker : markers) {
        appendIfAppropriate(marker, sb);
      }
    }
  }

  private void appendIfAppropriate(Marker marker, StringBuilder sb) {
    if (marker instanceof LogFmtMarker keyValueMarker) {
      keyValueMarker.forEach((k, v) -> {
        if (!NativeKey.isNativeKey(k)) {
          appendKeyValueAndEscape(sb, k, v);
        }
      });
    } else if (marker.hasReferences()) {
      Iterator<Marker> iter = marker.iterator();
      while (iter.hasNext()) {
        Marker m = iter.next();
        appendIfAppropriate(m, sb);
      }
    }
  }

  private void errorAppender(StringBuilder sb, ILoggingEvent event) {
    if (event.getThrowableProxy() != null) {
      String msg = ThrowableProxyUtil.asString(event.getThrowableProxy());
      List<Marker> markers = event.getMarkerList();
      if (markers != null) {
        msg = handleCustomCallbacks(markers, msg, ApplyCallbackFor.ERROR);
      }
      appendKeyValueAndEscape(sb, NativeKey.ERROR.toString(), msg);

      appendCustomCallbackKeysIfNotPresentYet(sb, markers, NativeKey.ERROR.toString());
    }
  }

  private void appendCustomCallbackKeysIfNotPresentYet(StringBuilder sb,
                                                       List<Marker> markers,
                                                       String currAppenderName) {
    List<KeyValueAppender> currAppenders = customAppenders != null
            ? customAppenders
            : defaultAppenders;
    KeyValueAppender customAppender = appenders.get("custom");
    int customAppenderIdx = currAppenders.indexOf(customAppender);
    KeyValueAppender currAppender = appenders.get(currAppenderName);
    int currAppenderIdx = currAppenders.indexOf(currAppender);
    // custom appender will automatically add any custom specified key/value pairs.
    // However, if a custom value is added to the key/value list after the custom appender was
    // processed, these values will not be added to the log line automatically. To load such
    // key/values we check if the current appender is defined after the custom appender and if so
    // will add those key/values that are not yet part of the log line manually
    if (customAppenderIdx < currAppenderIdx) {
      appendCustomCallbackKeys(sb, markers);
    }
  }

  private void appendCustomCallbackKeys(StringBuilder sb, List<Marker> markers) {
    if (markers != null) {
      for (Marker marker : markers) {
        if (marker instanceof LogFmtMarker logFmtMarker) {
          if (logFmtMarker.hasCallbacks()) {
            List<Map.Entry<String, Object>> definedKeys = logFmtMarker.getDefinedKeyValues();
            String curLogLine = sb.toString();
            for (Map.Entry<String, Object> keyVal : definedKeys) {
              if (!curLogLine.contains(keyVal.getKey() + "=")) {
                appendKeyValueAndEscape(sb, keyVal.getKey(), keyVal.getValue());
              }
            }
          }
        }
      }
    }
  }

  private void packageAppender(StringBuilder sb, ILoggingEvent event) {
    String className = getLastClassName(event.getCallerData());
    if (className != null) {
      int lastPointPosition = className.lastIndexOf('.');
      String pkg = lastPointPosition >= 0 ? className.substring(0, lastPointPosition) : "";
      appendKeyValueAndEscape(sb, NativeKey.PACKAGE.toString(), pkg);
    }
  }

  private void moduleAppender(StringBuilder sb, ILoggingEvent event) {
    String className = getLastClassName(event.getCallerData());
    if (className != null) {
      int lastPointPosition = className.lastIndexOf('.');
      String module = lastPointPosition >= 0
              ? className.substring(lastPointPosition + 1)
              : className;
      appendKeyValueAndEscape(sb, NativeKey.MODULE.toString(), module);
    }
  }

  private String getLastClassName(StackTraceElement[] callerData) {
    String className = null;
    if (callerData != null && callerData.length > 0) {
      className = callerData[0].getClassName();
    }
    return className;
  }

  @FunctionalInterface
  interface KeyValueAppender {
    void append(StringBuilder sb, ILoggingEvent event);
  }

  /**
   * Appends the given key and value to the given StringBuilder. The key and value will be in
   * LogFmt typical style like {@code key="value"}.
   *
   * <p>This implementation will ignore null or empty keys and add {@code "null"} for values with a
   * null-value. Non-null values will be escaped via {@link #escapeValue(String)} if needed first
   * and then put between two quotation marks ({@code "}) if quotation is needed.
   *
   * <p>If fields were specified within the {@code <maskPasswords>...</maskPasswords>} directive
   * of the configuration XML the values of matching keys will be replaced by <em>****</em>.
   */
  private void appendKeyValueAndEscape(StringBuilder sb, String key, Object value) {
    if (key == null || key.isEmpty()) {
      return;
    }

    if (value == null) {
      value = "null";
    }

    sb.append(key).append('=');

    String valueStr = value.toString();
    if (maskPasswords != null && maskPasswords.contains(key)) {
      valueStr = "***";
    }

    if (needsQuoting(valueStr)) {
      sb.append('"').append(escapeValue(valueStr)).append('"');
    } else {
      sb.append(valueStr);
    }

    sb.append(' ');
  }

  private static boolean needsQuoting(String value) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);

      if (!((c >= 'a' && c <= 'z')
          || (c >= 'A' && c <= 'Z')
          || (c >= '0' && c <= '9')
          || c == '-' || c == '.' || c == '_' || c == '/' || c == '@' || c == '^' || c == '+')) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a StringBuilder representing the given string with characters escaped.
   *
   * @link <a href="https://docs.oracle.com/javase/tutorial/java/data/characters.html">List
   *     of escaped characters</a>
   */
  public static StringBuilder escapeValue(String string) {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < string.length(); i++) {
      char c = string.charAt(i);
      switch (c) {
        case '\t' -> sb.append("\\t"); // tabulator
        case '\b' -> sb.append("\\b"); // back-space
        case '\n' -> sb.append("\\n"); // new line
        case '\r' -> sb.append("\\r"); // carriage return
        case '\f' -> sb.append("\\f"); // form-feed
        case '\"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        default -> sb.append(c);
      }
    }

    return sb;
  }

  private static String formatLogLevel(Level level) {
    return level.toString().toLowerCase();
  }
}
