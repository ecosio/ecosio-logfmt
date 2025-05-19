package com.ecosio.logfmt.internal.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.ecosio.logfmt.ApplyCallbackFor;
import com.ecosio.logfmt.internal.NativeKey;
import com.ecosio.logfmt.internal.State;
import com.ecosio.logfmt.utils.StringUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import org.slf4j.Marker;

/**
 * A message appender that will take care of appending the actual log message to the log line.
 */
public class MessageAppender extends KeyValueAppender {

  /**
   * Instantiates a new object of this class and assigns the passed in state object to its internal
   * state.
   *
   * @param state The internal state of the LogFMT layout formatter
   */
  public MessageAppender(@NonNull final State state) {
    super(state);
  }

  @Override
  public void append(@NonNull final StringBuilder sb, @NonNull final ILoggingEvent event) {
    String msg = event.getFormattedMessage();
    final List<Marker> markers = event.getMarkerList();
    if (markers != null) {
      msg = StringUtils.obfuscateMsgIfNeeded(markers, msg);
      msg = handleCustomCallbacks(markers, msg, ApplyCallbackFor.MESSAGE);
    }
    StringUtils.appendKeyValueAndEscape(
            sb, NativeKey.MESSAGE.toString(), msg, state.getMaskPasswords());

    appendCustomCallbackKeysIfNotPresentYet(sb, markers, NativeKey.MESSAGE.toString());
  }
}
