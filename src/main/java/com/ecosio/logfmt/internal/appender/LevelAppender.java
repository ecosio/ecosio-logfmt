package com.ecosio.logfmt.internal.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.ecosio.logfmt.internal.NativeKey;
import com.ecosio.logfmt.internal.State;
import com.ecosio.logfmt.utils.StringUtils;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A log level appender that will take care of appending the log level name to the log line.
 */
public class LevelAppender extends KeyValueAppender {

  /**
   * Instantiates a new object of this class and assigns the passed in state object to its internal
   * state.
   *
   * @param state The internal state of the LogFMT layout formatter
   */
  public LevelAppender(@NonNull final State state) {
    super(state);
  }

  @Override
  public void append(@NonNull final StringBuilder sb, @NonNull final ILoggingEvent event) {
    StringUtils.appendKeyValueAndEscape(sb, NativeKey.LEVEL.toString(),
            StringUtils.formatLogLevel(event.getLevel()));
  }
}
