module ecosio.logfmt {
  requires java.base;
  requires org.slf4j;
  requires ch.qos.logback.classic;
  requires ch.qos.logback.core;
  requires com.github.spotbugs.annotations;

  exports com.ecosio.logfmt;
  exports com.ecosio.logfmt.utils to ecosio.logfmt.test;
}