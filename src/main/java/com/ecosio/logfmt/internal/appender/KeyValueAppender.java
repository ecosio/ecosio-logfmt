package com.ecosio.logfmt.internal.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.ecosio.logfmt.ApplyCallbackFor;
import com.ecosio.logfmt.LogFmtMarker;
import com.ecosio.logfmt.internal.State;
import com.ecosio.logfmt.utils.StringUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import java.util.Map;
import org.slf4j.Marker;

/**
 * Functional interface for passing appender method references around. Implementations of this
 * interface will use the provided {@link ILoggingEvent} passed in via the {@link
 * #append(StringBuilder, ILoggingEvent)} method to extract particular key-value pairs to add to
 * the {@link StringBuilder} object.
 */
public abstract class KeyValueAppender {

  /**
   * The shared state of this LogFMT layout formatters.
   */
  protected State state;

  /**
   * Instantiates common state needed by appender.
   *
   * @param state An object holding the internal state of this LogFMT layout formatters
   */
  protected KeyValueAppender(@NonNull final State state) {
    this.state = state;
  }


  /**
   * Uses the event to extract values to add to the provided {@link StringBuilder} object.
   *
   * @param sb    The {@link StringBuilder} object to add the extracted key-value pairs to
   * @param event The event containing the data to extract by implementations of this interface
   */
  public abstract void append(@NonNull StringBuilder sb, @NonNull ILoggingEvent event);

  /**
   * Appends key-value pairs from any {@link LogFmtMarker} objects within the marker reference
   * chain to the content of the provided {@link StringBuilder} object.
   *
   * @param sb The {@link StringBuilder} object to add key-value pairs found in {@link LogFmtMarker}
   *           objects
   * @param markers The list of {@link Marker} objects that may hold additional key-value properties
   *                to add to the log line.
   * @param currAppenderName The name of the most recently processed appender. If the custom
   *                         appender was already processed before, any key-value pairs found in
   *                         {@link LogFmtMarker} objects not yet present in the log line will be
   *                         added to the provided string builder. If the custom appender was not
   *                         yet executed processing will be skipped as properties will be added by
   *                         that appender then
   */
  protected void appendCustomCallbackKeysIfNotPresentYet(@NonNull final StringBuilder sb,
                                                         @Nullable final List<Marker> markers,
                                                         @NonNull final String currAppenderName) {
    final List<KeyValueAppender> currAppenders = state.getAppender();
    final KeyValueAppender customAppender = state.getPredefinedAppender("custom");
    final int customAppenderIdx = currAppenders.indexOf(customAppender);
    final KeyValueAppender currAppender = state.getPredefinedAppender(currAppenderName);
    final int currAppenderIdx = currAppenders.indexOf(currAppender);
    // custom appender will automatically add any custom specified key/value pairs.
    // However, if a custom value is added to the key/value list after the custom appender was
    // processed, these values will not be added to the log line automatically. To load such
    // key/values we check if the current appender is defined after the custom appender and if so
    // will add those key/values that are not yet part of the log line manually
    if (customAppenderIdx < currAppenderIdx) {
      appendCustomCallbackKeys(sb, markers);
    }
  }

  private void appendCustomCallbackKeys(@NonNull final StringBuilder sb,
                                        @Nullable final List<Marker> markers) {
    if (markers != null) {
      for (final Marker marker : markers) {
        if (marker instanceof LogFmtMarker logFmtMarker
                && logFmtMarker.hasCallbacks()) {
          handleCallbackMarker(sb, logFmtMarker);
        }
      }
    }
  }

  private void handleCallbackMarker(@NonNull final StringBuilder sb,
                                    @NonNull final LogFmtMarker logFmtMarker) {
    final List<Map.Entry<String, Object>> definedKeys = logFmtMarker.getDefinedKeyValues();
    final String curLogLine = sb.toString();
    for (final Map.Entry<String, Object> keyVal : definedKeys) {
      if (!curLogLine.contains(keyVal.getKey() + "=")) {
        StringUtils.appendKeyValueAndEscape(
                sb, keyVal.getKey(), keyVal.getValue(), state.getMaskPasswords());
      }
    }
  }

  /**
   * Applies the provided <em>applyFor</em> callback on each {@link LogFmtMarker} object within the
   * provided list of <me>markers</me>. The callback called will receive the provided message as
   * input.
   *
   * <p>Note that if multiple {@link LogFmtMarker} formatters might be within the list of markers,
   * subsequent {@link LogFmtMarker} will receive the output of the previous marker as input.
   *
   * @param markers The list of markers to check for available callbacks
   * @param message The message to pass to the callback when markers are found that contain a
   *                callback
   * @param applyFor The actual callback to apply
   * @return The updated String by the callbacks or the original message string if no callback was
   *         found
   */
  @NonNull
  protected String handleCustomCallbacks(@NonNull final List<Marker> markers,
                                         @NonNull final String message,
                                         @NonNull final ApplyCallbackFor applyFor) {
    String msg = message;
    for (final Marker marker : markers) {
      if (marker instanceof LogFmtMarker logFmtMarker
              && logFmtMarker.hasCallbacks()) {
        msg = logFmtMarker.applyCallbackFor(msg, applyFor);
      }
    }

    return msg;
  }
}
