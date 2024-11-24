package org.mitre.synthea.codebase.generated.Stock;

import java.util.*;

import org.mitre.synthea.codebase.*;
import org.mitre.synthea.codebase.mapping.*;


public class StockMapping {

 private CodeMap codeMap;

 public StockMapping(CodeMap codeMap){
    this.codeMap = codeMap;
 }


 public List<Combo> generateNdcCombinations(int n) {
    // Étape 1 : Initialiser les listes
    List<String> vaccineGroupList = CodeMapUtil.extractVaccineGroupsFromCodebase(codeMap);
    List<String> ndcList = CodeMapUtil.extractNDCsFromCodebase(codeMap);
    List<Combo> combinations = new ArrayList<>();

    // Associer chaque NDC à ses Vaccine Groups via CVX
    RelatedCode relatedCode = new RelatedCode(codeMap);
    Map<String, List<String>> ndcToVaccineGroups = new HashMap<>();

    for (String ndc : ndcList) {
        List<String> cvxCodes = relatedCode.getCvxCodesFromNdc(ndc); // Obtenir les CVX associés
        List<String> vaccineGroups = new ArrayList<>();
        for (String cvx : cvxCodes) {
            vaccineGroups.addAll(relatedCode.getVaccineGroupLabelsFromCvx(cvx));
        }
        ndcToVaccineGroups.put(ndc, vaccineGroups);
    }

    // Étape 2 : Trouver des combinaisons d'NDC couvrant au moins n fois chaque Vaccine Group
    Set<String> requiredGroups = new HashSet<>(vaccineGroupList); // Convertir en Set pour suivi
    List<NDC> selectedNdcList = new ArrayList<>();
    Map<String, Integer> groupCoverage = new HashMap<>();
    for (String group : requiredGroups) {
        groupCoverage.put(group, 0); // Initialiser la couverture à 0 pour chaque groupe
    }

    for (String ndc : ndcToVaccineGroups.keySet()) {
        List<String> groupsCoveredByNdc = ndcToVaccineGroups.get(ndc);

        boolean added = false;
        for (String group : groupsCoveredByNdc) {
            if (groupCoverage.get(group) < n) { // Si ce groupe n'est pas encore couvert n fois
                groupCoverage.put(group, groupCoverage.get(group) + 1);
                added = true;
            }
        }

        if (added) {
            // Ajouter cet NDC aux NDC sélectionnés
            selectedNdcList.add(new NDC(ndc));
        }

        // Vérifier si tous les groupes sont couverts
        boolean allCovered = true;
        for (String group : requiredGroups) {
            if (groupCoverage.get(group) < n) {
                allCovered = false;
                break;
            }
        }

        if (allCovered) {
            // Créer une combinaison finale et ajouter à la liste des résultats
            Combo combo = new Combo(selectedNdcList, true, 100.0);
            combinations.add(combo);
            break; // On a une couverture complète
        }
    }

    // Ajouter une combinaison si certains groupes ne sont pas complètement couverts
    if (combinations.isEmpty()) {
        double completionPercentage = (double) groupCoverage.values().stream().mapToInt(Integer::intValue).sum()
                / (requiredGroups.size() * n) * 100.0;
        Combo incompleteCombo = new Combo(selectedNdcList, false, completionPercentage);
        combinations.add(incompleteCombo);
    }

    return combinations;
}

    
}
