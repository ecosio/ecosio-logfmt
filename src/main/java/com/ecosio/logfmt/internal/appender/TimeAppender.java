package com.ecosio.logfmt.internal.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.ecosio.logfmt.internal.NativeKey;
import com.ecosio.logfmt.internal.State;
import com.ecosio.logfmt.utils.StringUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * An appender that will take care of appending the timestamp the log was generated at to the log
 * line.
 */
public class TimeAppender extends KeyValueAppender {

  /**
   * Default date and time format.
   */
  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

  /**
   * Instantiates a new object of this class and assigns the passed in state object to its internal
   * state.
   *
   * @param state The internal state of the LogFMT layout formatter
   */
  public TimeAppender(@NonNull final State state) {
    super(state);
  }

  @Override
  public void append(@NonNull final StringBuilder sb, @NonNull final ILoggingEvent event) {
    final ThreadLocal<SimpleDateFormat> simpleDateFormat = ThreadLocal.withInitial(
            () -> new SimpleDateFormat(state.getTimeFormat() != null
                    ? state.getTimeFormat() : DATE_FORMAT,
                    Locale.getDefault()));

    StringUtils.appendKeyValueAndEscape(sb, NativeKey.TIME.toString(),
            simpleDateFormat.get().format(new Date(event.getTimeStamp())));
  }
}
