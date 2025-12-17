package org.mitre.synthea.codebase.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateHelper {


  private static final Logger logger = LoggerFactory.getLogger(DateHelper.class);
  //Within the codebase project, we will use a DateTime object, and use
  //a single date format.
  
  private final DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyyMMdd");

  public DateTime getDateTimeFromString(String codebaseDate) {

    try {
      DateTime dt = DateTime.parse(codebaseDate, dtf);
      return dt;
    } catch (IllegalArgumentException iae) {
      logger.debug("Not an expected date format: " + codebaseDate);
    }

    return null;

  }

}
