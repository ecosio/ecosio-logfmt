package com.ecosio.logfmt;

/**
 * The admissible fields a callback can be configured for.
 */
public enum ApplyCallbackFor {
  /**
   * Specifies that a callback should preprocess a segment that is intended for being logged as
   * <em>msg</em> field in the generated output.
   */
  MESSAGE,
  /**
   * Specifies that a callback should preprocess a segment that is intended for being logged as
   * <em>error</em> field in the generated output.
   */
  ERROR
}
