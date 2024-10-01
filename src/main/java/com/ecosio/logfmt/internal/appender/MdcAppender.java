package com.ecosio.logfmt.internal.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.ecosio.logfmt.internal.NativeKey;
import com.ecosio.logfmt.internal.State;
import com.ecosio.logfmt.utils.StringUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;

/**
 * A  message diagnostic context (MDC) appender that will take care of appending key-value pairs
 * found in the MDC context to the log line.
 */
public class MdcAppender extends KeyValueAppender {

  /**
   * Instantiates a new object of this class and assigns the passed in state object to its internal
   * state.
   *
   * @param state The internal state of the LogFMT layout formatter
   */
  public MdcAppender(@NonNull final State state) {
    super(state);
  }

  @Override
  public void append(@NonNull final StringBuilder sb, @NonNull final ILoggingEvent event) {
    final Map<String, String> mdc = event.getMDCPropertyMap();
    if (mdc != null) {
      mdc.forEach((k, v) -> {
        if (!NativeKey.isNativeKey(k)) {
          StringUtils.appendKeyValueAndEscape(sb, k, v, state.getMaskPasswords());
        }
      });
    }
  }
}
