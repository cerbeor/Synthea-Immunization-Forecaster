package org.mitre.synthea.codebase.reference;

import java.util.HashMap;
import java.util.Map;

public enum CodesetType {
  ACKNOWLEDGEMENT_TYPE("ACKNOWLEDGEMENT_TYPE", "Acknowledgement Type"),
  ADDRESS_TYPE("ADDRESS_TYPE", "Address Type"),
  ADMINISTRATION_UNIT("ADMINISTRATION_UNIT", "Administration Unit"),
  BIRTH_ORDER("BIRTH_ORDER", "Birth Order"),
  BODY_ROUTE("BODY_ROUTE", "Body Route"),
  BODY_SITE("BODY_SITE", "Body Site"),
  CONTRAINDICATION_OR_PRECAUTION("CONTRAINDICATION_OR_PRECAUTION",
      "Contraindication or Precaution"),
  EVIDENCE_OF_IMMUNITY("EVIDENCE_OF_IMMUNITY", "Evidence of Immunity"),
  FACILITY_TYPE("FACILITY_TYPE", "Facility Type"),
  FINANCIAL_STATUS_CODE("FINANCIAL_STATUS_CODE", "Financial Status Code"),
  FINANCIAL_STATUS_OBS_METHOD("FINANCIAL_STATUS_OBS_METHOD", "Financial Status Obs Method"),
  FORECAST_IMMUNIZATION_SCHEDULE("FORECAST_IMMUNIZATION_SCHEDULE",
      "Forecast Immunization Schedule"),
  FORECAST_REASON("FORECAST_REASON", "Forecast Reason"),
  FORECAST_SERIES_STATUS("FORECAST_SERIES_STATUS", "Forecast Series Status"),
  HL7_CODING_SYSTEM("HL7_CODING_SYSTEM", "HL7 Coding System"),
  HL7_ERROR_STATUS_CODE("HL7_ERROR_STATUS_CODE", "HL7 Error Status Code"),
  HL7_VALUE_TYPE("HL7_VALUE_TYPE", "HL7 Value Type"),
  ID_ASSIGNING_AUTHORITY("ID_ASSIGNING_AUTHORITY", "Id Assigning Authority"),
  ID_TYPE_CODE("ID_TYPE_CODE", "Id Type Code"),
  INJECTION_AMOUNT("INJECTION_AMOUNT", "Injection Amount"),
  INJECTION_GUIDANCE("INJECTION_GUIDANCE", "Injection Guidance"),
  INJECTION_GUIDANCE_SITE("INJECTION_GUIDANCE_SITE", "Injection Guidance Site"),
  MESSAGE_PROCESSING_ID("MESSAGE_PROCESSING_ID", "Message Processing Id"),
  MESSAGE_PROFILE_ID("MESSAGE_PROFILE_ID", "Message Profile Id"),
  OBSERVATION_IDENTIFIER("OBSERVATION_IDENTIFIER", "Observation Identifier"),
  PATIENT_CLASS("PATIENT_CLASS", "Patient Class"),
  PATIENT_ETHNICITY("PATIENT_ETHNICITY", "Patient Ethnicity"),
  PATIENT_ID("PATIENT_ID", "Patient Id"),
  PATIENT_PROTECTION("PATIENT_PROTECTION", "Patient Protection"),
  PATIENT_PUBLICITY("PATIENT_PUBLICITY", "Patient Publicity"),
  PATIENT_RACE("PATIENT_RACE", "Patient Race"),
  PATIENT_SEX("PATIENT_SEX", "Patient Sex"),
  PERSON_LANGUAGE("PERSON_LANGUAGE", "Person Language"),
  PERSON_NAME_TYPE("PERSON_NAME_TYPE", "Person Name Type"),
  PERSON_RELATIONSHIP("PERSON_RELATIONSHIP", "Person Relationship"),
  PHYSICIAN_NUMBER("PHYSICIAN_NUMBER", "Physician Number"),
  POTENTIAL_ISSUE("POTENTIAL_ISSUE", "Potential Issue"),
  REGISTRY_STATUS("REGISTRY_STATUS", "Registry Status"),
  TELECOMMUNICATION_EQUIPMENT("TELECOMMUNICATION_EQUIPMENT", "Telecommunication Equipment"),
  TELECOMMUNICATION_USE("TELECOMMUNICATION_USE", "Telecommunication Use"),
  VACCINATION_ACTION_CODE("VACCINATION_ACTION_CODE", "Vaccination Action Code"),
  VACCINATION_COMPLETION("VACCINATION_COMPLETION", "Vaccination Completion"),
  VACCINATION_CONFIDENTIALITY("VACCINATION_CONFIDENTIALITY", "Vaccination Confidentiality"),
  VACCINATION_CPT_CODE("VACCINATION_CPT_CODE", "Vaccination CPT Code"),
  VACCINATION_CVX_CODE("VACCINATION_CVX_CODE", "Vaccination CVX Code"),
  VACCINATION_FUNDING_SOURCE("VACCINATION_FUNDING_SOURCE", "Vaccination Funding Source"),
  VACCINATION_INFORMATION_SOURCE("VACCINATION_INFORMATION_SOURCE",
      "Vaccination Information Source"),
  VACCINATION_LOT_NUMBER_PATTERN("VACCINATION_LOT_NUMBER_PATTERN", "Vaccination Lot Number Pattern"),
  VACCINATION_MANUFACTURER_CODE("VACCINATION_MANUFACTURER_CODE", "Vaccination Manufacturer Code"),
  VACCINATION_ORDER_CONTROL_CODE("VACCINATION_ORDER_CONTROL_CODE",
      "Vaccination Order Control Code"),
  VACCINATION_REACTION("VACCINATION_REACTION", "Vaccination Reaction"),
  VACCINATION_REFUSAL("VACCINATION_REFUSAL", "Vaccination Refusal"),
  VACCINATION_SPECIAL_INDICATIONS("VACCINATION_SPECIAL_INDICATIONS",
      "Vaccination Special Indications"),
  VACCINATION_TRADE_NAME("VACCINATION_TRADE_NAME", "Vaccination Trade Name"),
  VACCINATION_VALIDITY("VACCINATION_VALIDITY", "Vaccination Validity"),
  VACCINATION_VIS_CVX_CODE("VACCINATION_VIS_CVX_CODE", "Vaccination VIS CVX Code"),
  VACCINATION_VIS_DOC_TYPE("VACCINATION_VIS_DOC_TYPE", "Vaccination VIS Doc Type"),
  VACCINATION_VIS_VACCINES("VACCINATION_VIS_VACCINES", "Vaccination VIS Vaccines"),
  VACCINE_GROUP("VACCINE_GROUP", "Vaccine Group"),
  VACCINE_PRODUCT("VACCINE_PRODUCT", "Vaccine Product"),
  VACCINE_TYPE("VACCINE_TYPE", "Vaccine Type"),
  VACCINATION_NDC_CODE_UNIT_OF_SALE("VACCINATION_NDC_CODE_UNIT_OF_SALE",
      "Vaccination NDC Code Unit of Sale"),
  VACCINATION_NDC_CODE_UNIT_OF_USE("VACCINATION_NDC_CODE_UNIT_OF_USE",
      "Vaccination NDC Code Unit of Use"),
  VACCINATION_NDC_CODE("VACCINATION_NDC_CODE", "Vaccination NDC Code (Use or Sale)"),
//This is not actually in the XML. It represents the combination of sale and use.
  ;

  private String description;
  private String typeName;

  private static final Map<String, CodesetType> descMap = new HashMap<String, CodesetType>();
  private static final Map<String, CodesetType> typeMap = new HashMap<String, CodesetType>();

  static {
    for (CodesetType t : CodesetType.values()) {
      descMap.put(t.description, t);
    }
    for (CodesetType t : CodesetType.values()) {
      typeMap.put(t.typeName, t);
    }
  }

  private CodesetType(String typeName, String desc) {
    this.description = desc;
    this.typeName = typeName;
  }

  public String getDescription() {
    return description;
  }

  public String getType() {
    return typeName;
  }

  public static CodesetType getByDesc(String desc) {
    return descMap.get(desc);
  }


  public static CodesetType getByTypeCode(String type) {
    return typeMap.get(type);
  }

}
