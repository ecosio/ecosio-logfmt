package com.ecosio.logfmt.utils;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Helper class for stacktrace related operations.
 */
public final class StacktraceHelper {

  private StacktraceHelper() {

  }

  /**
   * Returns the name of the last {@link StackTraceElement}.
   *
   * @param callerData The stacktrace information to extract the name of the last caller
   * @return The name of the top-most class on the stack
   */
  @Nullable
  public static String getLastClassName(@Nullable final StackTraceElement ... callerData) {
    String className = null;
    if (callerData != null && callerData.length > 0) {
      className = callerData[0].getClassName();
    }
    return className;
  }
}
