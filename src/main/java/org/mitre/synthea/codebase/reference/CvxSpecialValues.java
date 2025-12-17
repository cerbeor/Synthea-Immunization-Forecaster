package org.mitre.synthea.codebase.reference;

import java.util.HashMap;
import java.util.Map;

public enum CvxSpecialValues {
  NO_VACCINE_ADMINISTERED("998"),
  UNKNOWN("999"),
  NORMAL("any other.  This is the default");
  private static final Map<String, CvxSpecialValues> codeMap = new HashMap<String, CvxSpecialValues>();

  static {
    for (CvxSpecialValues t : CvxSpecialValues.values()) {
      codeMap.put(t.value, t);
    }
  }

  private String value;

  private CvxSpecialValues(String valueIn) {
    this.value = valueIn;
  }

  public static CvxSpecialValues getBy(String value) {
    CvxSpecialValues val = codeMap.get(value);
    if (val != null) {
      return val;
    }
    return NORMAL;
  }

  public String getCvxCode() {
    return value;
  }

}
