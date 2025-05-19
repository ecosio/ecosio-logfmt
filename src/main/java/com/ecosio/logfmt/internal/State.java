package com.ecosio.logfmt.internal;

import com.ecosio.logfmt.internal.appender.CustomFieldsAppender;
import com.ecosio.logfmt.internal.appender.ErrorAppender;
import com.ecosio.logfmt.internal.appender.KeyValueAppender;
import com.ecosio.logfmt.internal.appender.LevelAppender;
import com.ecosio.logfmt.internal.appender.MdcAppender;
import com.ecosio.logfmt.internal.appender.MessageAppender;
import com.ecosio.logfmt.internal.appender.ModuleAppender;
import com.ecosio.logfmt.internal.appender.PackageAppender;
import com.ecosio.logfmt.internal.appender.ThreadAppender;
import com.ecosio.logfmt.internal.appender.TimeAppender;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the internal state of this class which was set either through default values or by
 * configuring values through the logback configuration.
 */
public class State {

  /**
   * A mapping of property names and their assigned appender methods.
   */
  private final Map<String, KeyValueAppender> appender = new ConcurrentHashMap<>();
  /**
   * The default list of appender. The order of appender defines the order in which key-value pairs
   * are written to the log line. First appender in the list will add key-value pairs first to the
   * log line.
   */
  private final List<KeyValueAppender> defaultAppender;
  /**
   * Defines a user specified list of appender. This list may contain the optional prefix and app
   * appender as well as leave out other unneeded appender. First appender in the list will add
   * key-value pairs first to the log line.
   */
  private List<KeyValueAppender> customAppender;

  /**
   * List of property names whose value segment needs to be obfuscated with three asterix symbols
   * (<code>***</code>).
   */
  private List<String> maskPasswords;
  /**
   * The specified time format to log time values in.
   */
  private String timeFormat;

  /**
   * Initializes a new state object and configures the set of default appender.
   */
  public State() {
    appender.put(NativeKey.TIME.toString(), new TimeAppender(this));
    appender.put(NativeKey.LEVEL.toString(), new LevelAppender(this));
    appender.put(NativeKey.MESSAGE.toString(), new MessageAppender(this));
    appender.put(NativeKey.THREAD.toString(), new ThreadAppender(this));
    appender.put("package", new PackageAppender(this));
    appender.put("module", new ModuleAppender(this));
    appender.put("mdc", new MdcAppender(this));
    appender.put("custom", new CustomFieldsAppender(this));
    appender.put(NativeKey.ERROR.toString(), new ErrorAppender(this));

    this.defaultAppender = List.of(
            appender.get(NativeKey.TIME.toString()),
            appender.get(NativeKey.LEVEL.toString()),
            appender.get(NativeKey.THREAD.toString()),
            appender.get("package"),
            appender.get("module"),
            appender.get(NativeKey.MESSAGE.toString()),
            appender.get("mdc"),
            appender.get("custom"),
            appender.get(NativeKey.ERROR.toString())
    );
  }

  /**
   * Returns the mapping of internal appender name and the reference to the actual appender object.
   *
   * @return The map holding the appender name as key and the actual appender as value
   */
  @Nullable
  public KeyValueAppender getPredefinedAppender(@NonNull final String name) {
    return appender.get(name);
  }

  /**
   * Returns the list of appender which are used by this LogFMT layout formatter.
   *
   * @return The list of appender to use
   */
  @NonNull
  public List<KeyValueAppender> getAppender() {
    return customAppender != null ? customAppender : defaultAppender;
  }

  /**
   * Specifies the list of custom appender to use. Note that the order of appender will define the
   * order in which key-value pairs appear within the final log line. Appender listed first will get
   * their key-value pairs added to the log line before subsequent appender.
   *
   * @param customAppender A list of key-value appender to use
   */
  public void setCustomAppender(@NonNull final List<KeyValueAppender> customAppender) {
    this.customAppender = List.copyOf(customAppender);
  }

  /**
   * Specifies the key names of key-value pairs within the log line that should be masked by three
   * asterix (<code>***</code>) to obfuscate the actual secret credential.
   *
   * @param maskPasswords The list of keys whose value part needs to be masked
   */
  public void setMaskPasswords(@Nullable final List<String> maskPasswords) {
    if (maskPasswords == null) {
      this.maskPasswords = List.of();
    } else {
      this.maskPasswords = List.copyOf(maskPasswords);
    }
  }

  /**
   * Returns the list of key names whose value segment needs to be masked.
   *
   * @return The list of key names corresponding values need to get masked
   */
  @Nullable
  public List<String> getMaskPasswords() {
    return maskPasswords;
  }

  /**
   * Specifies the new time format for the timestamp to set on the log line.
   *
   * @param timeFormat The new timestamp format to apply
   */
  public void setTimeFormat(@Nullable final String timeFormat) {
    this.timeFormat = timeFormat;
  }

  /**
   * Returns the current time format specified.
   *
   * @return The currently specified timestamp format
   */
  @Nullable
  public String getTimeFormat() {
    return this.timeFormat;
  }
}
