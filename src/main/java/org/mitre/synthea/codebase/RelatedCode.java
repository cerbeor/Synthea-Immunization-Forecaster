package org.mitre.synthea.codebase;

import java.util.ArrayList;
import java.util.List;
import org.mitre.synthea.codebase.generated.*;
import org.mitre.synthea.codebase.reference.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelatedCode {

  private static final Logger logger = LoggerFactory.getLogger(RelatedCode.class);

  private final CodeMap map;

  public RelatedCode(CodeMap cm) {
    this.map = cm;
  }

  public List<String> getVaccineGroupLabelsFromCvx(String cvx) {
    List<String> grouplabels = new ArrayList<>();
    List<Code> cvxVaccineGroups = this.map
        .getRelatedCodesForCodeIn(CodesetType.VACCINATION_CVX_CODE, cvx, CodesetType.VACCINE_GROUP);
    if (cvxVaccineGroups != null) {
      for (Code c : cvxVaccineGroups) {
        if (c != null) {
          grouplabels.add(c.getLabel());
        }
      }
    }
    return grouplabels;
  }

  public String getCvxValueFromNdcString(String ndcStringIn) {
    String cvxValue = "";
    Code ndc = this.map.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE, ndcStringIn);
    cvxValue = this.map.getRelatedValue(ndc, CodesetType.VACCINATION_CVX_CODE);
    return cvxValue;
  }

  public String getCvxFromCptString(String cptIn) {
    Code cpt = this.map.getCodeForCodeset(CodesetType.VACCINATION_CPT_CODE, cptIn);
    String cvxValue = this.map.getRelatedValue(cpt, CodesetType.VACCINATION_CVX_CODE);
    return cvxValue;
  }

    public List<String> getNdcFromMultipleCvx(List<String> cvxCodes) {
      List<String> ndcs = new ArrayList<>();
      for (String cvx : cvxCodes) {
          // Retrieve the CVX code
          Code cvxCode = this.map.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, cvx);
          // If the CVX code exists, get the associated NDC codes
          if (cvxCode != null) {
              // Use a method to retrieve the associated NDC codes
              // Ensure that you have a method that fetches the linked codes (e.g., NDC for CVX)
              String ndcValue = this.map.getRelatedValue(cvxCode, CodesetType.VACCINATION_NDC_CODE);
              if (ndcValue != null && !ndcs.contains(ndcValue)) {
                  ndcs.add(ndcValue);
              }
          }
      }
      return ndcs;
  }

  // New method to retrieve multiple NDCs from a single CVX
  public List<String> getNdcCodesFromCvx(String cvx) {
      List<String> ndcCodes = new ArrayList<>();
      // Retrieve related NDC codes for the given CVX
      List<Code> relatedNdcCodes = this.map.getRelatedCodesForCodeIn(CodesetType.VACCINATION_CVX_CODE, cvx, CodesetType.VACCINATION_NDC_CODE);
      
      if (relatedNdcCodes != null) {
          for (Code ndcCode : relatedNdcCodes) {
              if (ndcCode != null) {
                  // Add the value of the NDC code to the list
                  ndcCodes.add(ndcCode.getValue());
              }
          }
      }
      return ndcCodes;
  }


  //

    /**
   * Retrieves a list of CVX codes associated with a given NDC.
   *
   * @param ndc The NDC code as a string.
   * @return A list of CVX codes (strings) related to the provided NDC.
   */
  public List<String> getCvxCodesFromNdc(String ndc) {
    List<String> cvxCodes = new ArrayList<>();
    // Retrieve the NDC code from the map
    Code ndcCode = this.map.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE, ndc);

    if (ndcCode != null) {
        // Retrieve related CVX codes for the NDC
        List<Code> relatedCvxCodes = this.map.getRelatedCodesForCodeIn(CodesetType.VACCINATION_NDC_CODE, ndc, CodesetType.VACCINATION_CVX_CODE);

        if (relatedCvxCodes != null) {
            for (Code cvxCode : relatedCvxCodes) {
                if (cvxCode != null) {
                    // Add the CVX code value to the list
                    cvxCodes.add(cvxCode.getValue());
                }
            }
        }
    }
    return cvxCodes;
  }



}
