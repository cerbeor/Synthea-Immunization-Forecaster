package org.mitre.synthea.codebase.generated.Stock;

import java.util.*;
import java.util.stream.Collectors;

import org.mitre.synthea.codebase.*;
import org.mitre.synthea.codebase.mapping.*;


public class StockMapping {

 private CodeMap codeMap;
 private HashMap<String, Boolean> mapStockVaccineGroup;

 public StockMapping(CodeMap codeMap){
    this.codeMap = codeMap;
 }


 public List<Combo> generateNdcCombinations(int n) {
    // Step 1: Initialize vaccine group and NDC lists
    List<String> vaccineGroupList = CodeMapUtil.extractVaccineGroupsFromCodebase(codeMap);
    List<String> ndcList = CodeMapUtil.extractNDCsFromCodebase(codeMap);
    List<Combo> combinations = new ArrayList<>();

    // Associate each NDC with its vaccine groups using CVX codes
    RelatedCode relatedCode = new RelatedCode(codeMap);
    Map<String, List<String>> ndcToVaccineGroups = new HashMap<>();

    for (String ndc : ndcList) {
        // Get the CVX codes linked to the NDC
        List<String> cvxCodes = relatedCode.getCvxCodesFromNdc(ndc);
        List<String> vaccineGroups = new ArrayList<>();
        for (String cvx : cvxCodes) {
            // Convert CVX codes to vaccine group labels
            vaccineGroups.addAll(relatedCode.getVaccineGroupLabelsFromCvx(cvx));
        }
        // Map the NDC to its associated vaccine groups
        ndcToVaccineGroups.put(ndc, vaccineGroups);
    }

    // Step 2: Generate all possible combinations of NDCs
    List<NDC> ndcObjects = ndcList.stream().map(NDC::new).collect(Collectors.toList());
    generateCombinations(new ArrayList<>(), ndcObjects, vaccineGroupList, n, combinations, ndcToVaccineGroups);

    // Sort combinations by effectiveness (e.g., fewer NDCs, higher score)
    combinations.sort(Comparator.comparingDouble(Combo::getScore).reversed());

    // Step 3: Separate and filter results based on vaccine group coverage
    List<Combo> fullyCoveredCombos = combinations.stream()
        .filter(Combo::isAllVaccineGroupsFound) // Only keep combos with full vaccine group coverage
        .collect(Collectors.toList());

    // If at least one fully covered combo exists, return only those
    if (!fullyCoveredCombos.isEmpty()) {
        return fullyCoveredCombos;
    }

    // Otherwise, return all combinations
    return combinations;
}

// Recursive method to generate combinations
private void generateCombinations(List<NDC> currentCombo, List<NDC> remainingNdcList,
                                  List<String> targetGroups, int n, List<Combo> resultCombos,
                                  Map<String, List<String>> ndcToVaccineGroups) {
    // Check if the current combination covers the required groups at least 'n' times
    Map<String, Integer> groupCoverage = new HashMap<>();
    for (String group : targetGroups) {
        groupCoverage.put(group, 0); // Initialize coverage to 0 for each group
    }

    for (NDC ndc : currentCombo) {
        // Get the vaccine groups covered by the current NDC
        List<String> coveredGroups = ndcToVaccineGroups.get(ndc.getNdcCode());
        for (String group : coveredGroups) {
            groupCoverage.put(group, groupCoverage.getOrDefault(group, 0) + 1);
        }
    }

    // Check if all vaccine groups are covered at least 'n' times
    boolean allGroupsCovered = groupCoverage.values().stream().allMatch(count -> count >= n);

    if (allGroupsCovered) {
        // If fully covered, create a complete combo and add to the result list
        double completionPercentage = 100.0; // All groups are fully covered
        Combo completeCombo = new Combo(new ArrayList<>(currentCombo), false, completionPercentage);
        completeCombo.setAllVaccineGroupsFound(true); // Mark vaccine groups as fully covered
        resultCombos.add(completeCombo);
        return; // No need to explore further in this branch
    }

    // If there are no remaining NDCs, create an incomplete combo
    if (remainingNdcList.isEmpty()) {
        double totalCoverage = (double) groupCoverage.values().stream().mapToInt(Integer::intValue).sum();
        double maxCoverage = targetGroups.size() * n;
        double completionPercentage = (totalCoverage / maxCoverage) * 100.0;
        Combo incompleteCombo = new Combo(new ArrayList<>(currentCombo), false, completionPercentage);
        incompleteCombo.setAllVaccineGroupsFound(false); // Mark as not fully covered
        resultCombos.add(incompleteCombo);
        return;
    }

    // Take one NDC from the remaining list and continue exploring combinations
    for (int i = 0; i < remainingNdcList.size(); i++) {
        NDC selectedNdc = remainingNdcList.get(i);
        currentCombo.add(selectedNdc); // Add this NDC to the current combination
        List<NDC> newRemainingList = remainingNdcList.subList(i + 1, remainingNdcList.size());
        generateCombinations(currentCombo, newRemainingList, targetGroups, n, resultCombos, ndcToVaccineGroups);
        currentCombo.remove(currentCombo.size() - 1); // Remove this NDC to explore other combinations
    }
}

// Initialize the mapStockVaccineGroup based on a random or fixed selection of a combo
public void initializeStockVaccineGroup(List<Combo> combos, boolean random) {
    // Initialize mapStockVaccineGroup as empty, with all NDCs set to false by default
    mapStockVaccineGroup = new HashMap<>();

    // Step 1: Select the combo based on the 'random' flag
    Combo selectedCombo;
    if (random) {
        Random rand = new Random();
        // Randomly select a Combo from the list of combos
        selectedCombo = combos.get(rand.nextInt(combos.size()));
    } else {
        // Select the first combo in the list if random is false
        selectedCombo = combos.get(0);
    }

    // Step 2: Get the NDCs from the selected combo
    List<NDC> selectedNdcList = selectedCombo.getNdcList();

    // Step 3: Mark the NDCs from the selected combo as true in the mapStockVaccineGroup
    for (NDC ndc : selectedNdcList) {
        mapStockVaccineGroup.put(ndc.getNdcCode(), true);  // Set selected NDC to true
    }

    // Step 4: Set all other NDCs in the mapStockVaccineGroup to false (default)
    for (String ndcCode : CodeMapUtil.extractNDCsFromCodebase(codeMap)) {
        if (!mapStockVaccineGroup.containsKey(ndcCode)) {
            mapStockVaccineGroup.put(ndcCode, false);  // Set non-selected NDCs to false
        }
    }
}


public HashMap<String, Boolean> getMapStockVaccineGroup() {
    return mapStockVaccineGroup;
}


public void setMapStockVaccineGroup(HashMap<String, Boolean> mapStockVaccineGroup) {
    this.mapStockVaccineGroup = mapStockVaccineGroup;
}


    
}
