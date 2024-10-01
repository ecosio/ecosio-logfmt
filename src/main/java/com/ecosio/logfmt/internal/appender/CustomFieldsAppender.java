package com.ecosio.logfmt.internal.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.ecosio.logfmt.internal.State;
import com.ecosio.logfmt.utils.StringUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import org.slf4j.Marker;

/**
 * A custom fields appender uses {@link com.ecosio.logfmt.LogFmtMarker LogFmtMarker} instances to
 * read key-value pairs from these objects and add those entries to the log line.
 */
public class CustomFieldsAppender extends KeyValueAppender {

  /**
   * Instantiates a new object of this class and assigns the passed in state object to its internal
   * state.
   *
   * @param state The internal state of the LogFMT layout formatter
   */
  public CustomFieldsAppender(@NonNull final State state) {
    super(state);
  }

  @Override
  public void append(@NonNull final StringBuilder sb, @NonNull final ILoggingEvent event) {
    final List<Marker> markers = event.getMarkerList();
    if (markers != null) {
      for (final Marker marker : markers) {
        StringUtils.appendIfAppropriate(marker, sb, state.getMaskPasswords());
      }
    }
  }
}
