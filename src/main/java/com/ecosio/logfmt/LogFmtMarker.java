/*
 * Copyright (C) 2023-2024 ecosio
 * All rights reserved
 */

package com.ecosio.logfmt;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.slf4j.Marker;

/**
 * A {@link Marker} implementation that allows to add further properties to a log statement. This
 * marker will be picked up by {@link LogFmtLayout} on generating the log message in logfmt format.
 *
 * <p>This implementation supports adding references to other markers which will be processed in
 * {@link #forEach(BiConsumer)} before the key-values assigned to this marker. Currently though
 * only chaining <em>LogFmtMarker</em> is supported and any other markers will be ignored.
 *
 * <p>A typical scenario on using this marker may look like this:
 *
 * <pre><code>
 * Marker base = LogFmtMarker.with("nodeName", nodeName).and("containerName", containerName);
 * Marker marker = LogFmtMarker.with("Test Marker", "key1", val1);
 * marker.add(base);
 * log.info(marker, "Some log statement");</code></pre>
 *
 * <p>In this setup we define a root or base marker that contains values that can be reused by other
 * <em>LogFmtMarker</em> instances. This base marker is then added to a further marker that
 * defines yet other key-values to be added to the log statement and then the marker is added to
 * the log statement itself. This will result in {@link LogFmtLayout} to include the key-values
 * of the base marker, <em>nodeName</em> and <em>containerName</em>, before adding <em>key1</em>
 * as custom fields to the log statement.
 *
 * <p>Since version <em>1.0.3</em> a customization hook can be configured which can be used to
 * preprocess segments that target either <em>msg</em> or <em>error</em> fields in the generated
 * output. A callback can be defined which i.e. filters unwanted parts from that value and could
 * also be used to expose parts of the value string via new fields. Callbacks can be configured via
 * {@link #withCustomized(ApplyCallbackFor, BiFunction)},
 * {@link #withCustomized(String, ApplyCallbackFor, BiFunction)} on the <em>LogFmtMarker</em>
 * class directly, which will create a new instance of this class along the way, or via
 * {@link #andCallback(ApplyCallbackFor, BiFunction)} on an existing instance directly.
 */
public class LogFmtMarker implements Marker {

  /**
   * Creates a new marker instance using its default <em>LOGFMT</em> name and assigns a key and
   * value to it as property.
   *
   * @param key A String based key to add to this marker
   * @param value The value belonging to the key
   * @return The reference to the new marker instance
   */
  public static LogFmtMarker with(String key, Object value) {
    return new LogFmtMarker().and(key, value);
  }

  /**
   * Creates a new marker instance using its default <em>LOGFMT</em> name and assigns a key and
   * value to it as property.
   *
   * @param key An Object based key to add to this marker
   * @param value The value belonging to the key
   * @return The reference to the new marker instance
   */
  public static LogFmtMarker with(Object key, Object value) {
    return new LogFmtMarker().and(key, value);
  }

  /**
   * Creates a new marker instance using the given name and assigns a key and value to it as
   * property.
   *
   * @param name The name of the marker instance
   * @param key A String based key to add to this marker
   * @param value The value belonging to the key
   * @return The reference to the new marker instance
   */
  public static LogFmtMarker with(String name, String key, Object value) {
    return new LogFmtMarker(name).and(key, value);
  }

  /**
   * Creates a new marker instance using the given name and assigns a key and value to it as
   * property.
   *
   * @param name The name of the marker instance
   * @param key An Object based key to add to this marker
   * @param value The value belonging to the key
   * @return The reference to the new marker instance
   * @throws IllegalArgumentException if <em>name</em> is null or empty
   */
  public static LogFmtMarker with(String name, Object key, Object value) {
    return new LogFmtMarker(name).and(key, value);
  }

  /**
   * Creates a new marker instance using its default <em>LOGFMT</em> name and assigns it a
   * callback hook for customizing the log output for a particular field identified by
   * {@link ApplyCallbackFor}.
   *
   * <p>The callback will receive the log message as it would be logged without any preprocessing
   * applied as well as a reference of the internally managed key and value segments that make up
   * a log message.
   *
   * <p>The callback can be used to either filter out unwanted stuff from the log segment the
   * callback is applied for or, as demonstrated with the sample code below, move parts of the
   * respective log message to a separate field.
   *
   * <pre><code>
   * Marker marker = LogFmtMarker.withCustomized(LogFmtMarker.ApplyCallbackFor.MESSAGE,
   *         (msg, keyValues) -> {
   *           String separator = "-----";
   *           int idx = msg.lastIndexOf(separator) + separator.length();
   *           String history = msg.substring(0, idx);
   *           String message = msg.substring(idx + 1);
   *
   *           keyValues.add(new LogFmtMarker.KeyValue("history", history));
   *           return message;
   *         });
   * </code></pre>
   *
   * <p>In the above sample the original value to log is taken and modified by removing the
   * history part of that log message and storing it as new <em>history</em> field.
   *
   * <p>If multiple {@link LogFmtMarker} objects are chained and multiple customizations are
   * present for the same field, i.e. the <em>msg</em> field, subsequent instances will receive
   * the modified log message as input.
   *
   * @param applyFor The field to apply the customization for. Currently, only
   *                 {@link ApplyCallbackFor#MESSAGE} and {@link ApplyCallbackFor#ERROR} fields
   *                 are supported
   * @param callback The callback to apply before generating the log statement. If multiple
   *                 chained callbacks for the same field are present, consecutive callbacks will
   *                 receive the output of the previous callback as input. The second input argument
   *                 will be a list of key-value entries the current marker has configured. This
   *                 can be used to add further customized keys and their value to the log line
   * @return The initialized marker
   * @since 1.0.3
   */
  public static LogFmtMarker withCustomized(ApplyCallbackFor applyFor,
                                            BiFunction<String, List<KeyValue>, String> callback) {
    return new LogFmtMarker().andCallback(applyFor, callback);
  }

  /**
   * Creates a new marker instance using its specified marker name and assigns it a callback hook
   * for customizing the log output for a particular field identified by {@link ApplyCallbackFor}.
   *
   * <p>The callback will receive the log message as it would be logged without any preprocessing
   * applied as well as a reference of the internally managed key and value segments that make up
   * a log message.
   *
   * <p>The callback can be used to either filter out unwanted stuff from the log segment the
   * callback is applied for or, as demonstrated with the sample code below, move parts of the
   * respective log message to a separate field.
   *
   * <pre><code>
   * Marker marker = LogFmtMarker.withCustomized(
   *         "CONFIDENTIAL",
   *         LogFmtMarker.ApplyCallbackFor.MESSAGE,
   *         (msg, keyValues) -> {
   *           String separator = "-----";
   *           int idx = msg.lastIndexOf(separator) + separator.length();
   *           String history = msg.substring(0, idx);
   *           String message = msg.substring(idx + 1);
   *
   *           keyValues.add(new LogFmtMarker.KeyValue("history", history));
   *           return message;
   *         });
   * </code></pre>
   *
   * <p>In the above sample the original value to log is taken and modified by removing the
   * history part of that log message and storing it as new <em>history</em> field.
   *
   * <p>If multiple {@link LogFmtMarker} objects are chained and multiple customizations are
   * present for the same field, i.e. the <em>msg</em> field, subsequent instances will receive
   * the modified log message as input.
   *
   * @param applyFor The field to apply the customization for. Currently, only
   *                 {@link ApplyCallbackFor#MESSAGE} and {@link ApplyCallbackFor#ERROR} fields
   *                 are supported
   * @param callback The callback to apply before generating the log statement. If multiple
   *                 chained callbacks for the same field are present, consecutive callbacks will
   *                 receive the output of the previous callback as input. The second input argument
   *                 will be a list of key-value entries the current marker has configured. This
   *                 can be used to add further customized keys and their value to the log line
   * @return The initialized marker
   * @since 1.0.3
   */
  public static LogFmtMarker withCustomized(String name,
                                            ApplyCallbackFor applyFor,
                                            BiFunction<String, List<KeyValue>, String> callback) {
    return new LogFmtMarker(name).andCallback(applyFor, callback);
  }

  /**
   * A key/value entry for parts within a log statement.
   *
   * @param key The key to add to the log statement
   * @param value The value to set for the key in the log statement
   */
  public record KeyValue(String key, Object value) {

  }

  private final String name;
  private final List<Marker> references = new ArrayList<>();
  private final List<KeyValue> keyValues = new LinkedList<>();
  private final Map<ApplyCallbackFor, BiFunction<String, List<KeyValue>, String>> callbacks =
          new HashMap<>();

  private LogFmtMarker() {
    this("LOGFMT");
  }

  private LogFmtMarker(String name) {
    checkParam(name, "Invalid marker name");
    this.name = name;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void add(Marker reference) {
    checkParam(reference, "Attempted to add invalid marker reference");
    references.add(reference);
  }

  @Override
  public boolean remove(Marker reference) {
    return references.remove(reference);
  }

  @Override
  @Deprecated
  public boolean hasChildren() {
    return !references.isEmpty();
  }

  @Override
  public boolean hasReferences() {
    return !references.isEmpty();
  }

  @Override
  public Iterator<Marker> iterator() {
    return references.iterator();
  }

  @Override
  public boolean contains(Marker other) {
    return references.contains(other);
  }

  @Override
  public boolean contains(String name) {
    return references.stream().anyMatch(m -> m.getName().equals(name));
  }

  /**
   * Specifies whether callback were configured with the current marker or not.
   *
   * @return <code>true</code> if at least one callback was configured with the current marker;
   *         <code>false</code> if no callback was configured
   * @since 1.0.3
   */
  boolean hasCallbacks() {
    return !callbacks.isEmpty();
  }

  /**
   * Checks if a callback for the configured target field is present and in case one is present
   * will apply the callback by providing it with the current log segment value and a reference
   * to the internally used key-value list that are then converted to a log message.
   *
   * @param valueToLog The value of the original message or error to log or the output of any
   *                   preceding callback that was applied for that field
   * @param applyFor Specifies the target field to apply the callback for
   * @return Returns the output of the callback in case a callback for the given target field is
   *         available or the unmodified <em>valueToLog</em> value if no callback was applied
   * @since 1.0.3
   */
  // package private on purpose
  String applyCallbackFor(String valueToLog, ApplyCallbackFor applyFor) {
    BiFunction<String, List<KeyValue>, String> callback = callbacks.get(applyFor);
    if (callback != null) {
      return callback.apply(valueToLog, keyValues);
    }
    return valueToLog;
  }

  /**
   * Return the currently defined keys by this marker instance.
   *
   * @return The defined keys of this marker
   */
  List<Map.Entry<String, Object>> getDefinedKeyValues() {
    return keyValues.stream()
            .map(kv -> new AbstractMap.SimpleEntry<>(kv.key, kv.value))
            .collect(Collectors.toList());
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof LogFmtMarker other)) {
      return false;
    }
    return name.equals(other.name)
            && keyValues.equals(other.keyValues)
            && references.equals(other.references);
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + name.hashCode();
    result = 31 * result + keyValues.hashCode();
    result = 31 * result + references.hashCode();
    return result;
  }

  @Override
  public String toString() {
    String refs;
    if (references.isEmpty()) {
      refs = "";
    } else {
      refs = ", refs: " + references.stream().map(Marker::getName).collect(Collectors.joining());
    }
    return "LogFmt-Marker: " + name + " [keyVal: "
            + keyValues.stream().map(r -> r.key + "=" + r.value).collect(Collectors.joining())
            + refs
            + "]";
  }

  /**
   * Adds a new key-value entry to this marker. If the key is null or empty the key and value
   * will be ignored and not added.
   *
   * @param key A String based key to add to the marker
   * @param value The value for the provided key
   * @return The LogFmt marker object
   */
  public LogFmtMarker and(String key, Object value) {
    if (key != null && !"".equals(key)) {
      this.keyValues.add(new KeyValue(key, value));
    }
    return this;
  }

  /**
   * Adds a new key-value entry to this marker. If the key is null or empty the key and value
   * will be ignored and not added.
   *
   * @param key An object based key to add to the marker
   * @param value The value for the provided key
   * @return The LogFmt marker object
   */
  public LogFmtMarker and(Object key, Object value) {
    if (key != null && !"".equals(key)) {
      this.keyValues.add(new KeyValue(key.toString(), value));
    }
    return this;
  }

  /**
   * Adds a hook into the log value generation to customize the value that is written to the
   * specified field identified by  {@link ApplyCallbackFor}.
   *
   * <p>A callback is useful if some parts of the regular log line want to get filtered out. I.e.
   * the original log line produced may contain some history summary that should not be part of
   * the actual <em>msg</em> field but instead get exposed via an own <em>history</em> property
   * in the generated log line.
   *
   * <p>Let's assume that the original message starts with a history payload and contains a
   * dedicated separator and ends with the actual message to show in the log for the <em>msg</em>
   * field. The sample code below demonstrates how the history can get filtered from the actual
   * <em>msg</em> field and get exposed via its own
   *
   * <pre><code>
   * Marker marker = LogFmtMarker.withCustomized(LogFmtMarker.ApplyCallbackFor.MESSAGE,
   *         (msg, keyValues) -> {
   *           String separator = "-----";
   *           int idx = msg.lastIndexOf(separator) + separator.length();
   *           String history = msg.substring(0, idx);
   *           String message = msg.substring(idx + 1);
   *
   *           keyValues.add(new LogFmtMarker.KeyValue("history", history));
   *           return message;
   *         });
   * </code></pre>
   *
   * <p>In the above sample the original value to log is taken and modified by removing the
   * history part of that log message and storing it as new <em>history</em> field.
   *
   * <p>If multiple {@link LogFmtMarker} objects are chained, subsequent instances will receive
   * the modified log message as input.
   *
   * @param applyFor The field the callback should be applied for. The return statement of the
   *                 callback function will modify the original value for that field with the
   *                 returned output of the callback function
   * @param callback The callback to apply. The first <code>String</code> parameter is the
   *                 original log message that should be processed by this callback. If markers
   *                 are chained and each defines callbacks the value of a subsequent marker will
   *                 be the output of the previous one. The second argument of the callback is a
   *                 reference of the internal key/value list where new entries can get added to
   *                 inside the callback function
   * @return A reference to the {@link LogFmtMarker} instance the method was called on
   * @since 1.0.3
   */
  public LogFmtMarker andCallback(ApplyCallbackFor applyFor,
                                  BiFunction<String, List<KeyValue>, String> callback) {
    if (applyFor != null) {
      this.callbacks.put(applyFor, callback);
    }
    return this;
  }

  /**
   * Applies the given bi-consumer to all registered key-value pairs of this marker object.
   *
   * <p>This implementation will iterate through all referenced markers and apply the bi-consumer
   * before applying it on the key-values of the current marker instance. Note that currently
   * only <em>LogFmtMarker</em> objects are handled. Any other referenced markers are ignored.
   *
   * @param consumer The bi-consumer to act on the key-value pairs of this marker
   */
  public void forEach(BiConsumer<String, Object> consumer) {
    if (hasReferences()) {
      for (Marker marker : references) {
        if (marker instanceof LogFmtMarker base) {
          base.forEach(consumer);
        }
      }
    }
    keyValues.forEach(keyValue -> consumer.accept(keyValue.key, keyValue.value));
  }

  private void checkParam(Object obj, String msg) {
    if (obj == null || "".equals(obj)) {
      throw new IllegalArgumentException(msg);
    }
  }
}
