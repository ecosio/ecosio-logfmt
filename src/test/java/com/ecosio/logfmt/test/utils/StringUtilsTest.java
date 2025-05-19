package com.ecosio.logfmt.test.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.ecosio.logfmt.utils.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class StringUtilsTest {

  @Nested
  @DisplayName("escapes")
  public class EscapeTest {

    @Test
    @DisplayName("simple quoted string")
    public void escapeSimpleQuotedString() {
      assertThat(StringUtils.escapeValue("The \"message\"").toString(),
              is(equalTo("The \\\"message\\\"")));
    }

    @Test
    @DisplayName("strings containing carriage returns")
    public void carriageReturns() {
      assertThat(StringUtils.escapeValue("The \n carriage \n return").toString(),
              is(equalTo("The \\n carriage \\n return")));
    }

    @Test
    @DisplayName("strings containing tabulators")
    public void tabulators() {
      assertThat(StringUtils.escapeValue("The \t tab \t return").toString(),
              is(equalTo("The \\t tab \\t return")));
    }

    @Test
    @DisplayName("strings containing backslashes")
    public void backslashes() {
      assertThat(StringUtils.escapeValue("The \\ backslash \\ return").toString(),
              is(equalTo("The \\\\ backslash \\\\ return")));
    }
  }
}
