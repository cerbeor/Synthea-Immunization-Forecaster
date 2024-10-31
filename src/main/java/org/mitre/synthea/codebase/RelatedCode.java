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
        // Obtenez le code CVX
        Code cvxCode = this.map.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, cvx);
        // Si le code CVX existe, obtenez les NDC associés
        if (cvxCode != null) {
            // Ici, vous devrez utiliser une méthode qui récupère les NDC associés
            // Vous devrez avoir une méthode qui renvoie les codes liés (par exemple, NDC pour le CVX)
            String ndcValue = this.map.getRelatedValue(cvxCode, CodesetType.VACCINATION_NDC_CODE);
            if (ndcValue != null && !ndcs.contains(ndcValue)) {
                ndcs.add(ndcValue);
            }
        }
    }
    return ndcs;
}

  // Nouvelle méthode pour récupérer plusieurs NDC à partir d'un CVX
  public List<String> getNdcCodesFromCvx(String cvx) {
    List<String> ndcCodes = new ArrayList<>();
    List<Code> relatedNdcCodes = this.map.getRelatedCodesForCodeIn(CodesetType.VACCINATION_CVX_CODE, cvx, CodesetType.VACCINATION_NDC_CODE);
    
    if (relatedNdcCodes != null) {
      for (Code ndcCode : relatedNdcCodes) {
        if (ndcCode != null) {
          ndcCodes.add(ndcCode.getValue());
        }
      }
    }
    return ndcCodes;
  }

}
