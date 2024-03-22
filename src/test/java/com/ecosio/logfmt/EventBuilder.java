/*
 * Copyright (C) 2023-2024 ecosio
 * All rights reserved
 */

package com.ecosio.logfmt;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import org.slf4j.Marker;

public class EventBuilder {
  private final String msg;
  private final long time;
  private Level loggingLevel = Level.INFO;
  private Map<String, String> mdc;
  private List<Marker> markers;
  private Throwable error;

  public EventBuilder(String msg) {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2017, Calendar.NOVEMBER, 30, 15, 10, 25);
    this.time = calendar.getTime().getTime();
    this.msg = msg;
  }

  public EventBuilder loggingLevel(Level level) {
    this.loggingLevel = level;
    return this;
  }

  public EventBuilder mdc(Map<String, String> mdc) {
    this.mdc = mdc;
    return this;
  }

  public EventBuilder markers(Marker... markers) {
    this.markers = List.of(markers);
    return this;
  }

  public EventBuilder error(Throwable t) {
    this.error = t;
    return this;
  }

  public ILoggingEvent build() {
    ILoggingEvent event = mock(ILoggingEvent.class);
    when(event.getLevel()).thenReturn(loggingLevel);
    when(event.getTimeStamp()).thenReturn(time);
    when(event.getThreadName()).thenReturn("thread0");
    when(event.getFormattedMessage()).thenReturn(msg);
    when(event.getMDCPropertyMap()).thenReturn(mdc);
    when(event.getMarkerList()).thenReturn(markers);
    if (error != null) {
      IThrowableProxy proxy = new ThrowableProxy(error);
      when(event.getThrowableProxy()).thenReturn(proxy);
    }

    StackTraceElement[] stackTraceElements = new StackTraceElement[2];
    stackTraceElements[0] =
            new StackTraceElement("com.ecosio.logfmt.LogFmtLayout",
                    "doLayout", "LogFmtLayout.java", 215);
    stackTraceElements[1] =
            new StackTraceElement("com.ecosio.logfmt.LogFmtLayoutTest",
                    "build", "LogFmtLayoutTest.java", 101);
    when(event.getCallerData()).thenReturn(stackTraceElements);

    return event;
  }
}
