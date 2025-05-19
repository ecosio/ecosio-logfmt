package com.ecosio.logfmt.internal;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Native keys that are automatically added by the Layout.
 *
 * <p>Cannot be used with Markers and MDC!
 */
public enum NativeKey {
  /**
   * Marks the property as time value.
   */
  TIME("time"),
  /**
   * Marks the property as log-level value.
   */
  LEVEL("level"),
  /**
   * Marks the property as log message value.
   */
  MESSAGE("msg"),
  /**
   * Marks the property as application field value.
   */
  APP("app"),
  /**
   * Marks the property as thread name value.
   */
  THREAD("thread"),
  /**
   * Marks the property as package name value.
   */
  PACKAGE("package"),
  /**
   * Marks the property as module name value.
   */
  MODULE("module"),
  /**
   * Marks the property as error value.
   */
  ERROR("error");

  /**
   * Name of the property to include in the final logfmt formatted log-line.
   */
  private final String text;

  NativeKey(@NonNull final String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return text;
  }

  /**
   * Checks whether a given property identified by the key name belongs to the set of natively
   * supported property keys.
   *
   * @param key The property key name to check for whether it belongs to the native properties
   * @return <code>true</code> if the provided key is natively supported by this LOGFMT layout;
   *         <code>false</code> when the provided key belongs to a custom property
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public static boolean isNativeKey(@NonNull final String key) {
    final NativeKey[] keys = values();
    for (final NativeKey nativeKey : keys) {
      if (nativeKey.text.equals(key)) {
        return true;
      }
    }

    return false;
  }
}
