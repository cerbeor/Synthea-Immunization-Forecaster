package org.mitre.synthea.codebase.reference;

import java.util.HashMap;
import java.util.Map;
import org.mitre.synthea.codebase.generated.CodeStatus;


public enum CodeStatusValue {

  INVALID("INVALID"),
  UNRECOGNIZED("UNRECOGNIZED"),
  DEPRECATED("DEPRECATED"),
  IGNORED("IGNORED"),
  VALID("Valid");

  private static final Map<String, CodeStatusValue> descMap = new HashMap<String, CodeStatusValue>();

  static {
    for (CodeStatusValue t : CodeStatusValue.values()) {
      descMap.put(t.value.toUpperCase(), t);
    }
  }

  private String value;

  private CodeStatusValue(String valueIn) {
    this.value = valueIn;
  }

  public static CodeStatusValue getBy(String value) {
    if (value == null) {
      return CodeStatusValue.UNRECOGNIZED;
    }

    CodeStatusValue val = descMap.get(value.toUpperCase());

    if (val == null) {
      return CodeStatusValue.UNRECOGNIZED;
    }

    return val;
  }

  public static CodeStatusValue getBy(CodeStatus cs) {
    if (cs == null) {
      return VALID;
    }
    return getBy(cs.getStatus());
  }

}
