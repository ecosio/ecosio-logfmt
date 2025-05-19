package com.ecosio.logfmt.internal.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.ecosio.logfmt.internal.NativeKey;
import com.ecosio.logfmt.internal.State;
import com.ecosio.logfmt.utils.StacktraceHelper;
import com.ecosio.logfmt.utils.StringUtils;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An appender that will take care of appending the package name of the class that triggered the
 * logging event to the log line.
 */
public class PackageAppender extends KeyValueAppender {

  /**
   * Instantiates a new object of this class and assigns the passed in state object to its internal
   * state.
   *
   * @param state The internal state of the LogFMT layout formatter
   */
  public PackageAppender(@NonNull final State state) {
    super(state);
  }

  @Override
  public void append(@NonNull final StringBuilder sb, @NonNull final ILoggingEvent event) {
    final String className = StacktraceHelper.getLastClassName(event.getCallerData());
    if (className != null) {
      final int lastPointPosition = className.lastIndexOf('.');
      final String pkg = lastPointPosition >= 0 ? className.substring(0, lastPointPosition) : "";
      StringUtils.appendKeyValueAndEscape(sb, NativeKey.PACKAGE.toString(), pkg);
    }
  }
}
