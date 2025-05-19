module ecosio.logfmt.test {

  requires ecosio.logfmt;
  requires org.hamcrest;
  requires org.slf4j;
  requires org.mockito;
  requires ch.qos.logback.classic;
  requires transitive org.junit.jupiter.engine;
  requires transitive org.junit.jupiter.api;

  exports com.ecosio.logfmt.test;
  exports com.ecosio.logfmt.test.utils;
}