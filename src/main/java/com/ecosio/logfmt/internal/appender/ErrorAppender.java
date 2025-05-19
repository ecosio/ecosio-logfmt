package com.ecosio.logfmt.internal.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import com.ecosio.logfmt.ApplyCallbackFor;
import com.ecosio.logfmt.internal.NativeKey;
import com.ecosio.logfmt.internal.State;
import com.ecosio.logfmt.utils.StringUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import org.slf4j.Marker;

/**
 * An error appender that will take care of appending stacktrace information to the log line.
 */
public class ErrorAppender extends KeyValueAppender {

  /**
   * Instantiates a new object of this class and assigns the passed in state object to its internal
   * state.
   *
   * @param state The internal state of the LogFMT layout formatter
   */
  public ErrorAppender(@NonNull final State state) {
    super(state);
  }

  @Override
  public void append(@NonNull final StringBuilder sb, @NonNull final ILoggingEvent event) {
    if (event.getThrowableProxy() != null) {
      String msg = ThrowableProxyUtil.asString(event.getThrowableProxy());
      final List<Marker> markers = event.getMarkerList();
      if (markers != null) {
        msg = handleCustomCallbacks(markers, msg, ApplyCallbackFor.ERROR);
      }
      StringUtils.appendKeyValueAndEscape(sb, NativeKey.ERROR.toString(), msg);

      appendCustomCallbackKeysIfNotPresentYet(sb, markers, NativeKey.ERROR.toString());
    }
  }
}
