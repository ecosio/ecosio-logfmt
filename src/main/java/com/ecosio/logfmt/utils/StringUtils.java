package com.ecosio.logfmt.utils;

import ch.qos.logback.classic.Level;
import com.ecosio.logfmt.LogFmtMarker;
import com.ecosio.logfmt.internal.NativeKey;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Marker;

/**
 * Utility class to handle basic {@link String} checks and manipulations.
 */
public final class StringUtils {

  /**
   * The regex pattern to identify a Basic Authentication token which should be obfuscated.
   */
  private static final Pattern BASIC_AUTH_PATTERN =
          Pattern.compile("([Bb])asic ([a-zA-Z0-9+/=]{3})[a-zA-Z0-9+/=]*([a-zA-Z0-9+/=]{3})");
  /**
   * The regex pattern to identify a URL authentication <em>user:password</em> pair which should be
   * obfuscated.
   */
  private static final Pattern URL_AUTH_PATTERN = Pattern.compile("//(.*?):.*?@");

  private StringUtils() {

  }


  /**
   * Checks the provided marker for any key-value pairs that are not native to this LOGFMT layout
   * formatter and appends its key-value pairs after escaping to the provided {@link StringBuilder}
   * object.
   *
   * <p>If the marker itself is not a {@link LogFmtMarker} object it will follow any of its
   * references and attempt to read the properties from these markers.
   *
   * @param marker The {@link Marker} object to check for appendable key-value pair
   * @param sb The {@link StringBuilder} object to append the non-native key-value pairs to
   * @param maskPasswords Optional list of string values that will obfuscate the value of the
   *                      key-value pair if one of the values provided in the list match the actual
   *                      key name
   */
  public static void appendIfAppropriate(@Nullable final Marker marker,
                                         @NonNull final StringBuilder sb,
                                         @Nullable final List<String> maskPasswords) {
    if (marker == null) {
      return;
    }
    if (marker instanceof LogFmtMarker keyValueMarker) {
      keyValueMarker.forEach((k, v) -> {
        if (!NativeKey.isNativeKey(k)) {
          appendKeyValueAndEscape(sb, k, v, maskPasswords);
        }
      });
    } else if (marker.hasReferences()) {
      final Iterator<Marker> iter = marker.iterator();
      while (iter.hasNext()) {
        final Marker m = iter.next();
        appendIfAppropriate(m, sb, maskPasswords);
      }
    }
  }

  /**
   * Appends the given key and value to the given StringBuilder. The key and value will be in
   * LogFmt typical style like {@code key="value"}.
   *
   * <p>This implementation will ignore null or empty keys and add {@code "null"} for values with a
   * null-value. Non-null values will be escaped via {@link StringUtils#escapeValue(String)} if
   * needed first and then put between two quotation marks ({@code "}) if quotation is needed.
   *
   * <p>If fields were specified within the {@code <maskPasswords>...</maskPasswords>} directive
   * of the configuration XML the values of matching keys will be replaced by <em>****</em>.
   *
   * @param sb The {@link StringBuilder} object to append the key-value pair data to
   * @param key The actual key name of the key-value pair
   * @param value The actual value of the key-value pair
   */
  public static void appendKeyValueAndEscape(@NonNull final StringBuilder sb,
                                             @Nullable final String key,
                                             @Nullable final Object value) {
    appendKeyValueAndEscape(sb, key, value, null);
  }

  /**
   * Appends the given key and value to the given StringBuilder. The key and value will be in
   * LogFmt typical style like {@code key="value"}.
   *
   * <p>This implementation will ignore null or empty keys and add {@code "null"} for values with a
   * null-value. Non-null values will be escaped via {@link StringUtils#escapeValue(String)} if
   * needed first and then put between two quotation marks ({@code "}) if quotation is needed.
   *
   * <p>If fields were specified within the {@code <maskPasswords>...</maskPasswords>} directive
   * of the configuration XML the values of matching keys will be replaced by <em>****</em>.
   *
   * @param sb The {@link StringBuilder} object to append the key-value pair data to
   * @param key The actual key name of the key-value pair
   * @param value The actual value of the key-value pair
   * @param maskPasswords Optional list of string values that will obfuscate the value of the
   *                      key-value pair if one of the values provided in the list match the actual
   *                      key name
   */
  public static void appendKeyValueAndEscape(@NonNull final StringBuilder sb,
                                             @Nullable final String key,
                                             @Nullable final Object value,
                                             @Nullable final List<String> maskPasswords) {
    if (key == null || key.isEmpty()) {
      return;
    }

    Object val = value;
    if (val == null) {
      val = "null";
    }

    sb.append(key).append('=');

    String valueStr = val.toString();
    if (maskPasswords != null && maskPasswords.contains(key)) {
      valueStr = "***";
    }

    if (needsQuoting(valueStr)) {
      sb.append('"').append(escapeValue(valueStr)).append('"');
    } else {
      sb.append(valueStr);
    }

    sb.append(' ');
  }

  /**
   * Checks the list of markers for a confidential marker and on a match will replace any matching
   * parts of the provided string with three asterix symbols (<code>***</code>).
   *
   * @param markers The list of markers to check for a confidential one
   * @param msg The message to obfuscate when confidential marker matched a part of this string
   * @return The obfuscated message if a confidential message matched or the original message when
   *         either no confidential marker was present or the pattern did not match
   */
  @NonNull
  public static String obfuscateMsgIfNeeded(@NonNull final List<Marker> markers,
                                            @NonNull final String msg) {
    String reply = msg;
    final Optional<Marker> confidential = markers.stream()
            .filter(m -> "CONFIDENTIAL".equals(m.getName()) || m.contains("CONFIDENTIAL"))
            .findFirst();
    if (confidential.isPresent()) {
      // Authorization: Basic dXNlcjpwYXNzd29yZA==
      final Matcher basicAuthMatcher = BASIC_AUTH_PATTERN.matcher(msg);
      // sftp://user:password@host:port/path/to/resource
      final Matcher urlAuthMatcher = URL_AUTH_PATTERN.matcher(msg);

      if (basicAuthMatcher.find()) {
        reply = basicAuthMatcher.replaceAll("***");
      }
      if (urlAuthMatcher.find()) {
        reply = urlAuthMatcher.replaceAll("//$1:***@");
      }
    }
    return reply;
  }

  /**
   * Checks and returns whether a provided string value needs to be put between two quotation marks.
   *
   * @param value The string value to check
   * @return <code>true</code> if the string contains characters that need to be put between two
   *         quotes; <code>false</code> otherwise
   */
  public static boolean needsQuoting(@NonNull final String value) {
    final String validChars =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._/@^+";

    boolean needsQuoting = false;
    for (int i = 0; i < value.length(); i++) {
      if (validChars.indexOf(value.charAt(i)) == -1) {
        needsQuoting = true;
        break;
      }
    }
    return needsQuoting;
  }

  /**
   * Returns a StringBuilder representing the given string with characters escaped.
   *
   * @link <a href="https://docs.oracle.com/javase/tutorial/java/data/characters.html">List
   *     of escaped characters</a>
   */
  @NonNull
  public static StringBuilder escapeValue(@NonNull final String string) {
    final StringBuilder sb = new StringBuilder();

    for (int i = 0; i < string.length(); i++) {
      final char c = string.charAt(i);
      switch (c) {
        case '\t' -> sb.append("\\t"); // tabulator
        case '\b' -> sb.append("\\b"); // back-space
        case '\n' -> sb.append("\\n"); // new line
        case '\r' -> sb.append("\\r"); // carriage return
        case '\f' -> sb.append("\\f"); // form-feed
        case '\"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        default -> sb.append(c);
      }
    }

    return sb;
  }

  /**
   * Returns the lowercase name of the provided log level.
   *
   * @param level The log level to return the name to put into the formatted log line
   * @return The lower case name of the provided log level
   */
  @NonNull
  public static String formatLogLevel(@NonNull final Level level) {
    return level.toString().toLowerCase(Locale.getDefault());
  }
}
