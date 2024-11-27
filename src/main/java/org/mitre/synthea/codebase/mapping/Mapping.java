package org.mitre.synthea.codebase.mapping;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.mitre.synthea.codebase.CodeMap;
import org.mitre.synthea.codebase.generated.Code;
import org.mitre.synthea.codebase.reference.CodesetType;

public class Mapping {
    private CodeMap codeMap;

    // Constructor that takes a CodeMap instance
    public Mapping(CodeMap codeMap) {
        this.codeMap = codeMap;
    }

    // Method to create NDC objects related to a list of CVX codes
    public List<NDC> createNDCsFromCVX(List<Code> cvxCodes) {
        Map<String, NDC> ndcMap = new HashMap<>();

        for (Code cvxCode : cvxCodes) {
            List<String> relatedNDCs = codeMap.getRelatedValues(cvxCode, CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE);
            
            for (String ndcCode : relatedNDCs) {
                // Create or update the NDC object
                NDC ndc = ndcMap.getOrDefault(ndcCode, new NDC(ndcCode));
                ndc.getCvxCodes().add(cvxCode); // Add the CVX code to the NDC
                ndcMap.put(ndcCode, ndc);
            }
        }

        return new ArrayList<>(ndcMap.values());
    }



    /**
     * Creates combinations of NDCs to match the input list of CVX codes.
     */
    public List<Combo> createCombosFromNDCs(List<NDC> ndcList, List<Code> targetCvxList) {
    List<Combo> combos = new ArrayList<>();
    
    // Group NDCs by their CVX codes for efficient searching
    Map<Set<Code>, List<NDC>> groupedNDCs = new HashMap<>();
    for (NDC ndc : ndcList) {
        Set<Code> cvxSet = new HashSet<>(ndc.getCvxCodes());
        groupedNDCs.computeIfAbsent(cvxSet, k -> new ArrayList<>()).add(ndc);
    }

    // Use backtracking to find the best combinations
    findCombos(new ArrayList<>(), targetCvxList, groupedNDCs, combos);

    // Sort by the number of NDCs (fewer NDCs is better)
    combos.sort(Comparator.comparingInt(c -> c.getNdcList().size()));
    
    // Éliminer les doublons
    return removeDuplicateCombos(combos);
}

/**
 * Helper method to recursively find combinations of NDCs to match the target CVX list.
 */
private void findCombos(List<NDC> currentCombo, List<Code> remainingCvx, 
                        Map<Set<Code>, List<NDC>> groupedNDCs, List<Combo> combos) {
    if (remainingCvx.isEmpty()) {
        combos.add(new Combo(new ArrayList<>(currentCombo), true, 100.0));
        return;
    }

    for (Map.Entry<Set<Code>, List<NDC>> entry : groupedNDCs.entrySet()) {
        Set<Code> ndcCvxSet = entry.getKey();
        List<NDC> ndcGroup = entry.getValue();

        // Skip if this NDC overlaps with existing NDCs in the current combo
        if (currentCombo.stream().anyMatch(ndc -> !Collections.disjoint(ndc.getCvxCodes(), ndcCvxSet))) {
            continue;
        }

        // Check if this NDC can help cover the remaining CVX codes
        List<Code> intersectingCvx = remainingCvx.stream()
                .filter(ndcCvxSet::contains)
                .collect(Collectors.toList());

        if (!intersectingCvx.isEmpty()) {
            for (NDC ndc : ndcGroup) {
                currentCombo.add(ndc);
                List<Code> newRemaining = new ArrayList<>(remainingCvx);
                newRemaining.removeAll(ndc.getCvxCodes());
                findCombos(currentCombo, newRemaining, groupedNDCs, combos);
                currentCombo.remove(currentCombo.size() - 1);
            }
        }
    }
}

/**
 * Method to remove duplicate Combos from the list.
 */
private List<Combo> removeDuplicateCombos(List<Combo> combos) {
    Set<Set<String>> uniqueCombos = new HashSet<>();
    List<Combo> filteredCombos = new ArrayList<>();

    for (Combo combo : combos) {
        // Generate a set of NDC codes to identify duplicates
        Set<String> ndcCodesSet = combo.getNdcList().stream()
                                        .map(NDC::getNdcCode)
                                        .collect(Collectors.toSet());

        if (uniqueCombos.add(ndcCodesSet)) {
            filteredCombos.add(combo);
        }
    }

    return filteredCombos;
}
}