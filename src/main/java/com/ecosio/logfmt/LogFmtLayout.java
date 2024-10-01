package com.ecosio.logfmt;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;
import com.ecosio.logfmt.internal.NativeKey;
import com.ecosio.logfmt.internal.State;
import com.ecosio.logfmt.internal.appender.KeyValueAppender;
import com.ecosio.logfmt.utils.StringUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Logback Layout that format logs with logfmt format (ie. {@code level="debug" ... key1="value1"
 * key2="value2" ...})
 *
 * @author Guillaume PERRUDIN
 * @author Roman Vottner
 */
public class LogFmtLayout extends LayoutBase<ILoggingEvent> {

  /**
   * An optional prefix property value. If a prefix is specified it will always be added at the
   * start of the log line.
   */
  private String prefix;
  /**
   * An optional property named <code>app</code> that has the name of the application that produced
   * this log line as value.
   */
  private String appName;

  /**
   * The internal state of this layout formatter.
   */
  private final State state;

  /**
   * Instantiates a new SLF4J log layout class that will format log messages in the
   * <em>logfmt</em> format.
   *
   * <p>On initializing this layout will first register supported appender and then set up a list
   * of default appender which will be processed one by one for each log message.
   */
  public LogFmtLayout() {
    super();
    state = new State();
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
  public void setPrefix(@Nullable final String prefix) {
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
  public void setAppName(@Nullable final String appName) {
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
   * @throws IllegalAccessException If an invalid time format is passed in
   */
  public void setTimeFormat(@NonNull final String timeFormat) throws IllegalAccessException {
    new SimpleDateFormat(timeFormat, Locale.getDefault());
    state.setTimeFormat(timeFormat);
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
  public void setFields(@NonNull final String fields) {
    final List<KeyValueAppender> customAppender = new ArrayList<>();
    for (final String field : fields.split(",")) {
      final KeyValueAppender appender = state.getPredefinedAppender(field.trim());
      if (appender != null) {
        customAppender.add(appender);
      }
    }
    state.setCustomAppender(customAppender);
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
  public void setMaskPasswords(@NonNull final String propsToMask) {
    final List<String> maskPasswords = new ArrayList<>();
    Collections.addAll(maskPasswords, propsToMask.split(","));
    state.setMaskPasswords(maskPasswords);
  }

  @Override
  public String doLayout(@NonNull final ILoggingEvent event) {
    final StringBuilder sb = new StringBuilder();
    if (prefix != null) {
      sb.append("prefix=").append(prefix).append(' ');
    }
    if (appName != null) {
      StringUtils.appendKeyValueAndEscape(sb, NativeKey.APP.toString(), appName);
    }

    for (final KeyValueAppender keyValueAppender : state.getAppender()) {
      keyValueAppender.append(sb, event);
    }
    sb.setCharAt(sb.length() - 1, '\n');

    return sb.toString();
  }
}
