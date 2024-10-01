package com.ecosio.logfmt.internal.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.ecosio.logfmt.internal.NativeKey;
import com.ecosio.logfmt.internal.State;
import com.ecosio.logfmt.utils.StacktraceHelper;
import com.ecosio.logfmt.utils.StringUtils;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An appender that will take care of appending the actual class name which produced the log event
 * to the log line.
 */
public class ModuleAppender extends KeyValueAppender {

  /**
   * Instantiates a new object of this class and assigns the passed in state object to its internal
   * state.
   *
   * @param state The internal state of the LogFMT layout formatter
   */
  public ModuleAppender(@NonNull final State state) {
    super(state);
  }

  @Override
  public void append(@NonNull final StringBuilder sb, @NonNull final ILoggingEvent event) {
    final String className = StacktraceHelper.getLastClassName(event.getCallerData());
    if (className != null) {
      final int lastPointPosition = className.lastIndexOf('.');
      final String module = lastPointPosition >= 0
              ? className.substring(lastPointPosition + 1)
              : className;
      StringUtils.appendKeyValueAndEscape(sb, NativeKey.MODULE.toString(), module);
    }
  }
}
