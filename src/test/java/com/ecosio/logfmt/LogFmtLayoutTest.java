/*
 * Copyright (C) 2023-2024 ecosio
 * All rights reserved
 */

package com.ecosio.logfmt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@DisplayName("LogFmtLayout")
public class LogFmtLayoutTest {

  @Nested
  @DisplayName("escapes")
  public class EscapeTest {

    @Test
    @DisplayName("simple quoted string")
    public void escapeSimpleQuotedString() {
      assertThat(LogFmtLayout.escapeValue("The \"message\"").toString(),
              is(equalTo("The \\\"message\\\"")));
    }

    @Test
    @DisplayName("strings containing carriage returns")
    public void carriageReturns() {
      assertThat(LogFmtLayout.escapeValue("The \n carriage \n return").toString(),
              is(equalTo("The \\n carriage \\n return")));
    }

    @Test
    @DisplayName("strings containing tabulators")
    public void tabulators() {
      assertThat(LogFmtLayout.escapeValue("The \t tab \t return").toString(),
              is(equalTo("The \\t tab \\t return")));
    }

    @Test
    @DisplayName("strings containing backslashes")
    public void backslashes() {
      assertThat(LogFmtLayout.escapeValue("The \\ backslash \\ return").toString(),
              is(equalTo("The \\\\ backslash \\\\ return")));
    }
  }

  @Test
  @DisplayName("adds prefix when specified")
  public void prefix() {
    // Arrange
    ILoggingEvent event = new EventBuilder("test message").build();
    LogFmtLayout layout = new LogFmtLayout();
    layout.setPrefix("test");

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "prefix=test time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout msg=\"test message\"\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("adds app name when specified")
  public void appName() {
    // Arrange
    ILoggingEvent event = new EventBuilder("test message").build();
    LogFmtLayout layout = new LogFmtLayout();
    layout.setAppName("test-app");

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "app=test-app time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout msg=\"test message\"\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("supports customizing time format")
  public void customTimeFormat() {
    // Arrange
    ILoggingEvent event = new EventBuilder("test message").build();
    LogFmtLayout layout = new LogFmtLayout();
    layout.setTimeFormat("yyyy-MM-dd HH:mm:ssZ");

    // Act
    String result = layout.doLayout(event);

    // Assert
    // CI has different time format than my local one. That's why on CI timezone is set to UTC
    // which ends up with a zone offset of +0000 while on CET systems the offset is +0100
    String expected =
            "time=\"2017-11-30 15:10:25\\+0[0|1]00\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout msg=\"test message\"\n";
    assertThat(result, matchesPattern(expected));
  }

  @Test
  @DisplayName("adds MDC properties to log message")
  public void mdcProperties() {
    // Arrange
    Map<String, String> mdc = new HashMap<>();
    mdc.put("key1", "val1");
    mdc.put("key2", "val2");
    ILoggingEvent event = new EventBuilder("test message").mdc(mdc).build();
    LogFmtLayout layout = new LogFmtLayout();

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout msg=\"test message\" "
                    + "key1=val1 key2=val2\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("ignores invalid MDC property key")
  public void ignoresInvalidMdcPropertyKey() {
    // Arrange
    Map<String, String> mdc = new HashMap<>();
    mdc.put("", "val1");
    mdc.put("key2", "val2");
    ILoggingEvent event = new EventBuilder("test message").mdc(mdc).build();
    LogFmtLayout layout = new LogFmtLayout();

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout msg=\"test message\" "
                    + "key2=val2\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("adds custom properties from marker to log message")
  public void customMarkerProperties() {
    // Arrange
    Marker marker = LogFmtMarker.with("nodeName", "testNode").and("containerName", "testContainer");
    ILoggingEvent event = new EventBuilder("test message").markers(marker).build();
    LogFmtLayout layout = new LogFmtLayout();

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout msg=\"test message\" "
                    + "nodeName=testNode containerName=testContainer\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("supports nested markers")
  public void nestedMarkers() {
    // Arrange
    Marker base = LogFmtMarker.with("nodeName", "testNode").and("containerName", "testContainer");
    Marker marker = LogFmtMarker.with("childMarker", "ip", "8.8.8.8");
    marker.add(base);
    ILoggingEvent event =
            new EventBuilder("test message").loggingLevel(Level.DEBUG).markers(marker).build();
    LogFmtLayout layout = new LogFmtLayout();

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=debug thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout msg=\"test message\" "
                    + "nodeName=testNode containerName=testContainer ip=8.8.8.8\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("adds both MDC and marker properties with the same name")
  public void mdcAndCustomMarkerPropertiesWithSameKey() {
    // Arrange
    Map<String, String> mdc = new HashMap<>();
    mdc.put("key", "val1");
    Marker marker = LogFmtMarker.with("key", "val2");
    ILoggingEvent event = new EventBuilder("test message").mdc(mdc).markers(marker).build();
    LogFmtLayout layout = new LogFmtLayout();

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout msg=\"test message\" "
                    + "key=val1 key=val2\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("support preprocessing message fields")
  public void preprocessCustomizedMessageField() {
    // Arrange
    final String testMsg = "Failed delivery for (MessageId: ee980790-c1bb-11ee-a43b-c9fd07729bd1" +
            " on ExchangeId: ee980790-c1bb-11ee-a43b-c9fd07729bd1). Exhausted after delivery " +
            "attempt: 2 caught: java.lang.NullPointerException: host must not be null.\\n" +
            "\\n" +
            "Message History (source location and message history is disabled)\\n" +
            "---------------------------------------------------------------------------------------------------------------------------------------\\n" +
            "Source                                   ID                             Processor                                          Elapsed (ms)\\n" +
            "                                         sample-route-failing/bean01 from[file:///opt/some-file-to-process?antExclude=*.tm   1160398262\\n" +
            "\\t...\\n" +
            "                                         sample-route-failing/RemoteWri bean[com.acme.someClass.failingBean                           0\\n" +
            "\\n" +
            "Stacktrace\\n" +
            "--------------------------------------------------------------------------------------------------------------------------------------- " +
            "Part that should stay in the message";

    Marker marker = LogFmtMarker.withCustomized(ApplyCallbackFor.MESSAGE,
            (msg, keyValues) -> {
              if (msg.startsWith("Failed delivery for")) {
                String separator =
                        "---------------------------------------------------------------------------------------------------------------------------------------";
                int idx = msg.lastIndexOf(separator) + separator.length();
                String history = msg.substring(0, idx);
                String message = msg.substring(idx + 1);

                keyValues.add(new LogFmtMarker.KeyValue("history", history));
                return message;
              }
              return msg;
            });
    ILoggingEvent event = new EventBuilder(testMsg)
            .markers(marker)
            .build();
    LogFmtLayout layout = new LogFmtLayout();

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout "
                    + "msg=\"Part that should stay in the message\" "
                    + "history=\"Failed delivery for (MessageId: " +
                    "ee980790-c1bb-11ee-a43b-c9fd07729bd1"
                    + " on ExchangeId: ee980790-c1bb-11ee-a43b-c9fd07729bd1). Exhausted after delivery "
                    + "attempt: 2 caught: java.lang.NullPointerException: host must not be null.\\\\n"
                    + "\\\\n"
                    + "Message History (source location and message history is disabled)\\\\n"
                    + "---------------------------------------------------------------------------------------------------------------------------------------\\\\n"
                    + "Source                                   ID                             Processor                                          Elapsed (ms)\\\\n"
                    + "                                         sample-route-failing/bean01 from[file:///opt/some-file-to-process?antExclude=*.tm   1160398262\\\\n"
                    + "\\\\t...\\\\n"
                    + "                                         sample-route-failing/RemoteWri bean[com.acme.someClass.failingBean                           0\\\\n"
                    + "\\\\n"
                    + "Stacktrace\\\\n"
                    +
                    "---------------------------------------------------------------------------------------------------------------------------------------\"\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("adds error stacktrace if available")
  public void errors() {
    // Arrange
    ILoggingEvent event =
            new EventBuilder("test message").error(new Exception("fubar")).build();
    LogFmtLayout layout = new LogFmtLayout();

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout msg=\"test message\" "
                    + "error=\"java.lang.Exception: fubar\\n"
                    + "\\tat com.ecosio.logfmt.LogFmtLayoutTest.errors(LogFmtLayoutTest.java:";
    assertThat(result, startsWith(expected));
  }

  @Test
  @DisplayName("customized error")
  public void customized_errors() {
    // Arrange
    Marker marker = LogFmtMarker.withCustomized(ApplyCallbackFor.ERROR,
            (error, keyValues) -> {
              String separator = "Caused by:";
              if (error.contains(separator)) {
                int idx = error.lastIndexOf(separator);
                String err = error.substring(0, idx);
                String root = error.substring(idx + separator.length() + 1);

                keyValues.add(new LogFmtMarker.KeyValue("root", root));
                return err;
              }
              return error;
            });

    ILoggingEvent event =
            new EventBuilder("test message")
                    .error(new Exception("fubar", new Exception("root")))
                    .markers(marker)
                    .build();
    LogFmtLayout layout = new LogFmtLayout();

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout msg=\"test message\" "
                    + "error=\"java.lang.Exception: fubar\\n"
                    + "\\tat com.ecosio.logfmt.LogFmtLayoutTest.customized_errors(LogFmtLayoutTest.java:";
    assertThat(result, startsWith(expected));
    assertThat(result, containsString("root=\"java.lang.Exception: root\\n"));
  }

  @Test
  @DisplayName("respects custom field order")
  public void customFieldOrder() {
    // Arrange
    ILoggingEvent event = new EventBuilder("test message").build();
    LogFmtLayout layout = new LogFmtLayout();
    layout.setFields("time,level,msg,thread,mdc,custom,error");

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=info msg=\"test message\" thread=thread0\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("ignores unknown fields")
  public void unknownField() {
    // Arrange
    ILoggingEvent event = new EventBuilder("test message").build();
    LogFmtLayout layout = new LogFmtLayout();
    layout.setFields("unknownField,level,msg,thread");

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "level=info msg=\"test message\" thread=thread0\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("masks passwords in properties")
  public void maskPropertyPassword() {
    // Arrange
    Marker marker = LogFmtMarker.with("userKey", "uaj8SAXyovga").and("pw", "uDkCC3fiK6Dy");
    ILoggingEvent event = new EventBuilder("test message").markers(marker).build();
    LogFmtLayout layout = new LogFmtLayout();
    layout.setMaskPasswords("userKey,pw");

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout msg=\"test message\" "
                    + "userKey=\"***\" pw=\"***\"\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("masks basic auth in log messages when a CONFIDENTIAL marker is present")
  public void maskBasicAuthInLogMessageWithConfidentialMarker() {
    // Arrange
    Marker marker = MarkerFactory.getMarker("CONFIDENTIAL");
    ILoggingEvent event =
            new EventBuilder("""
                    GET /some/service
                    Host: example.com
                    Authorization: Basic dXNlcjE6c29tZVBhc3N3b3Jk

                    <html><head><title>Test Page</title></head><body>Test content</body></html>""")
                    .markers(marker)
                    .build();
    LogFmtLayout layout = new LogFmtLayout();

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout "
                    + "msg=\"GET /some/service\\\\nHost: example.com\\\\n"
                    + "Authorization: Basic dXN***3Jk\\\\n\\\\n"
                    + "<html><head><title>Test Page</title></head><body>Test content</body></html>\"\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("does not mask basic auth in log messages when no CONFIDENTIAL marker is present")
  public void doNotMaskBasicAuthInLogMessageWithoutConfidentialMarker() {
    // Arrange
    ILoggingEvent event =
            new EventBuilder("""
                    GET /some/service
                    Host: example.com
                    Authorization: Basic dXNlcjE6c29tZVBhc3N3b3Jk

                    <html><head><title>Test Page</title></head><body>Test content</body></html>""")
                    .build();
    LogFmtLayout layout = new LogFmtLayout();

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout "
                    + "msg=\"GET /some/service\\\\nHost: example.com\\\\n"
                    + "Authorization: Basic dXNlcjE6c29tZVBhc3N3b3Jk\\\\n\\\\n"
                    + "<html><head><title>Test Page</title></head><body>Test content</body></html>\"\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("masks password inside URL in log messages when a CONFIDENTIAL marker is present")
  public void maskPasswordInUrlInLogMessageWithConfidentialMarker() {
    // Arrange
    Marker marker = MarkerFactory.getMarker("CONFIDENTIAL");
    ILoggingEvent event =
            new EventBuilder("Using sftp://user1:secretPassword@some.server.com to connect to "
                    + "remote service")
                    .markers(marker)
                    .build();
    LogFmtLayout layout = new LogFmtLayout();

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout "
                    + "msg=\"Using sftp://user1:***@some.server.com to connect to "
                    + "remote service\"\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("do not mask password inside URL in log messages when no CONFIDENTIAL marker is " +
          "present")
  public void doNotMaskPasswordInUrlInLogMessageWithoutConfidentialMarker() {
    // Arrange
    ILoggingEvent event =
            new EventBuilder("Using sftp://user1:secretPassword@some.server.com to connect to "
                    + "remote service")
                    .build();
    LogFmtLayout layout = new LogFmtLayout();

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout "
                    + "msg=\"Using sftp://user1:secretPassword@some.server.com to connect to "
                    + "remote service\"\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("supports masking passwords with chained markers")
  public void maskPasswordsWithChainedMarkers() {
    // Arrange
    Marker confidential = MarkerFactory.getMarker("CONFIDENTIAL");
    Marker marker = LogFmtMarker.with("userKey", "uaj8SAXyovga").and("pw", "uDkCC3fiK6Dy");
    marker.add(confidential);
    ILoggingEvent event =
            new EventBuilder("Using sftp://user1:secretPassword@some.server.com to connect to "
                    + "remote service")
                    .markers(marker)
                    .build();
    LogFmtLayout layout = new LogFmtLayout();
    layout.setMaskPasswords("userKey,pw");

    // Act
    String result = layout.doLayout(event);

    // Assert
    String expected =
            "time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                    + "package=com.ecosio.logfmt module=LogFmtLayout "
                    + "msg=\"Using sftp://user1:***@some.server.com to connect to "
                    + "remote service\" "
                    + "userKey=\"***\" pw=\"***\"\n";
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  @DisplayName("supports masking passwords with chained markers when LogFmtMarker is added to a " +
          "conventional marker")
  public void maskPasswordsWithChainedMarkers_reverseOrder() {
    // Arrange
    Marker confidential = MarkerFactory.getMarker("CONFIDENTIAL");
    Marker marker = LogFmtMarker.with("userKey", "uaj8SAXyovga").and("pw", "uDkCC3fiK6Dy");
    try {
      confidential.add(marker);
      ILoggingEvent event =
              new EventBuilder("Using sftp://user1:secretPassword@some.server.com to connect to "
                      + "remote service")
                      .markers(confidential)
                      .build();
      LogFmtLayout layout = new LogFmtLayout();
      layout.setMaskPasswords("userKey,pw");

      // Act
      String result = layout.doLayout(event);

      // Assert
      String expected =
              "time=\"2017-11-30T15:10:25\" level=info thread=thread0 "
                      + "package=com.ecosio.logfmt module=LogFmtLayout "
                      + "msg=\"Using sftp://user1:***@some.server.com to connect to "
                      + "remote service\" "
                      + "userKey=\"***\" pw=\"***\"\n";
      assertThat(result, is(equalTo(expected)));
    } finally {
      // as we add the logfmt marker to the static confidential marker it will still be contained
      // in other methods that will retrieve the marker using the MarkerFactory! Hence, we remove
      // it manually after this test so it doesn't influence other tests
      confidential.remove(marker);
    }
  }
}
