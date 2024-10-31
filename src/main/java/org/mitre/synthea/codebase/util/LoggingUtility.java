package org.mitre.synthea.codebase.util;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingUtility {

  private static final Logger logger = LoggerFactory
      .getLogger(LoggingUtility.class);

  public String stringify(Object o) {
    return ReflectionToStringBuilder.toString(o);
  }
}
