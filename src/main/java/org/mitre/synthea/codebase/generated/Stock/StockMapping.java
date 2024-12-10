package org.mitre.synthea.codebase.generated.Stock;

import java.util.*;
import java.util.stream.Collectors;

import org.mitre.synthea.codebase.*;
import org.mitre.synthea.codebase.mapping.*;
import org.mitre.synthea.codebase.reference.CodesetType;
import org.mitre.synthea.codebase.generated.*;;


public class StockMapping {

 private CodeMap codeMap;

 private HashMap<String, Boolean> mapStockVaccineGroup;
 int LOADING = 0;

//
private HashMap<String, Boolean> mapStockVaccineGroupPreviousVersion;


 public StockMapping(CodeMap codeMap){
    this.codeMap = codeMap;
 }

// New version

/**
 * Initialize the mapStockVaccineGroup with all NDCs from the code map set to false,
 * and randomly select N NDCs per vaccine group to set them to true.
 *
 * @param vaccineGroupMap A map where the key is a vaccine group, and the value is a list of NDC objects.
 * @param codeMap The CodeMap instance to extract all NDCs.
 * @param n The number of random NDCs to select from each vaccine group.
 */
public void initializeAndSelectRandomNDCs(Map<String, List<NDC>> vaccineGroupMap, CodeMap codeMap, int n) {
    // Step 1: Initialize mapStockVaccineGroup with all NDCs set to false
    mapStockVaccineGroup = new HashMap<>();
    List<String> allNdcCodes = CodeMapUtil.extractNDCsFromCodebase(codeMap); // Fetch all NDCs
    for (String ndcCode : allNdcCodes) {
        mapStockVaccineGroup.put(ndcCode, false);
    }

    Random random = new Random();

    // Step 2: Iterate through each vaccine group and select N random NDCs
    for (Map.Entry<String, List<NDC>> entry : vaccineGroupMap.entrySet()) {
        List<NDC> ndcList = entry.getValue();

        if (!ndcList.isEmpty()) {
            // Shuffle the list of NDCs for randomness
            Collections.shuffle(ndcList, random);

            // Select up to N NDCs and mark them as true in mapStockVaccineGroup
            for (int i = 0; i < Math.min(n, ndcList.size()); i++) {
                NDC selectedNdc = ndcList.get(i);
                mapStockVaccineGroup.put(selectedNdc.getNdcCode(), true);
            }
        }
    }
}


/**
 * Finds all NDCs that are not linked to any vaccine group and provides statistics on the mappings.
 *
 * @param vaccineGroupMap A map where the key is a vaccine group, and the value is a list of NDC objects.
 * @param codeMap The CodeMap instance to extract all NDCs.
 * @return A list of NDC codes that are not associated with any vaccine group.
 */
public List<String> findNDCsWithoutVaccineGroup(Map<String, List<NDC>> vaccineGroupMap, CodeMap codeMap) {
    // Step 1: Extract all NDCs from the CodeMap
    List<String> allNdcCodes = CodeMapUtil.extractNDCsFromCodebase(codeMap);

    // Step 2: Collect all NDCs present in the vaccineGroupMap
    Set<String> linkedNdcCodes = new HashSet<>();
    int vaccineGroupsWithNDCs = 0;
    int vaccineGroupsWithoutNDCs = 0;

    for (Map.Entry<String, List<NDC>> entry : vaccineGroupMap.entrySet()) {
        List<NDC> ndcList = entry.getValue();
        if (ndcList.isEmpty()) {
            vaccineGroupsWithoutNDCs++;
        } else {
            vaccineGroupsWithNDCs++;
            for (NDC ndc : ndcList) {
                linkedNdcCodes.add(ndc.getNdcCode());
            }
        }
    }

    // Step 3: Find NDCs not present in the linkedNdcCodes set
    List<String> unlinkedNdcCodes = new ArrayList<>();
    for (String ndcCode : allNdcCodes) {
        if (!linkedNdcCodes.contains(ndcCode)) {
            unlinkedNdcCodes.add(ndcCode);
        }
    }

    // Step 4: Calculate statistics
    int totalNDCs = allNdcCodes.size();
    int linkedNDCCount = linkedNdcCodes.size();
    int unlinkedNDCCount = unlinkedNdcCodes.size();

    // Print statistics
    System.out.println("Mapping Statistics:");
    System.out.println(" - Total NDCs: " + totalNDCs);
    System.out.println(" - Linked NDCs: " + linkedNDCCount);
    System.out.println(" - Unlinked NDCs: " + unlinkedNDCCount);
    System.out.println(" - Vaccine Groups with NDCs: " + vaccineGroupsWithNDCs);
    System.out.println(" - Vaccine Groups without NDCs: " + vaccineGroupsWithoutNDCs);

    return unlinkedNdcCodes;
}



/**
 * Maps CVX codes to their associated Vaccine Groups and NDCs.
 *
 * This method fetches all CVX codes from the CodeMap, determines the Vaccine Groups
 * related to each CVX, and links the CVX codes to NDCs via the CodeMap relationships.
 *
 * @return A map where the key is the vaccine group (String) and the value is a list of associated NDC objects.
 */
public Map<String, List<NDC>> mapCvxToVaccineGroupsAndNDCs() {
    // Map that will contain each VaccineGroup as a key and a list of associated NDCs as the value
    Map<String, List<NDC>> vaccineGroupMap = new HashMap<>();

    // 1. Retrieve all CVX codes
    List<String> cvxCodes = CodeMapUtil.extractCvxFromCodebase(codeMap); // Get all CVX codes from the database
    System.out.println("Total CVX Codes: " + cvxCodes.size());

    // 2. Loop through each CVX code
    for (String cvx : cvxCodes) {
        // Retrieve the CVX Code object
        Code cvxCodeObj = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, cvx);
        
        if (cvxCodeObj != null) {
            // Step 2.1: Retrieve the list of NDCs associated with this CVX
            List<String> relatedNDCs = codeMap.getRelatedValues(cvxCodeObj, CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE);
            System.out.println("CVX " + cvx + " has " + relatedNDCs.size() + " related NDCs.");

            // Step 2.2: Retrieve the vaccine group(s) associated with this CVX
            List<String> vaccineGroups = getVaccineGroupLabelsFromCvx(cvx); // Method to get the vaccine groups for a CVX
            System.out.println("CVX " + cvx + " is associated with " + vaccineGroups.size() + " vaccine groups.");

            // For each vaccine group associated with this CVX
            for (String vaccineGroup : vaccineGroups) {
                // Get or create the list of NDCs for this VaccineGroup
                List<NDC> ndcList = vaccineGroupMap.computeIfAbsent(vaccineGroup, k -> new ArrayList<>());

                // Add the NDCs associated with this VaccineGroup
                for (String ndcCode : relatedNDCs) {
                    // Create a new NDC object or retrieve an existing one
                    NDC ndc = new NDC(ndcCode);

                    // Add the CVX Code to this NDC
                    ndc.getCvxCodes().add(cvxCodeObj);

                    // Add this NDC to the list for this vaccine group
                    ndcList.add(ndc);
                }
            }
        }
    }

    // Return the final map
    return vaccineGroupMap;
}

/**
 * Method to retrieve the vaccine groups associated with a given CVX code.
 *
 * @param cvx The CVX code for which to retrieve associated vaccine groups.
 * @return A list of vaccine group names associated with the CVX code.
 */
public List<String> getVaccineGroupLabelsFromCvx(String cvx) {
    List<String> grouplabels = new ArrayList<>();
    
    // Create an instance of RelatedCode to retrieve groups associated with the CVX
    RelatedCode relatedCode = new RelatedCode(codeMap);
    
    // Retrieve the vaccine group codes associated with this CVX
    List<String> cvxVaccineGroups = relatedCode.getVaccineGroupLabelsFromCvx(cvx);
    
    // Check if any groups were found
    if (cvxVaccineGroups != null) {
        // Add each vaccine group to the list
        grouplabels.addAll(cvxVaccineGroups);
    }
    
    // Return the list of vaccine group labels associated with the CVX
    return grouplabels;
}






 // Previous version


 public List<Combo> generateNdcCombinations(int n, int m) {
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
    generateCombinations(new ArrayList<>(), ndcObjects, vaccineGroupList, n, m, combinations, ndcToVaccineGroups);

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
                                  List<String> targetGroups, int n, int m, List<Combo> resultCombos,
                                  Map<String, List<String>> ndcToVaccineGroups) {
    // Stop early if the desired number of combinations has been reached
    if (m != -1 && resultCombos.size() >= m) {
        return;
    }

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
        // Stop early if the limit has been reached
        if (m != -1 && resultCombos.size() >= m) {
            return;
        }
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
        generateCombinations(currentCombo, newRemainingList, targetGroups, n, m, resultCombos, ndcToVaccineGroups);
        currentCombo.remove(currentCombo.size() - 1); // Remove this NDC to explore other combinations
    }
}


// Initialize the mapStockVaccineGroupPreviousVersion based on a random or fixed selection of a combo
public void initializeStockVaccineGroup(List<Combo> combos, boolean random) {
    // Initialize mapStockVaccineGroupPreviousVersion as empty, with all NDCs set to false by default
    mapStockVaccineGroupPreviousVersion = new HashMap<>();

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

    // Step 3: Mark the NDCs from the selected combo as true in the mapStockVaccineGroupPreviousVersion
    for (NDC ndc : selectedNdcList) {
        mapStockVaccineGroupPreviousVersion.put(ndc.getNdcCode(), true);  // Set selected NDC to true
    }

    // Step 4: Set all other NDCs in the mapStockVaccineGroupPreviousVersion to false (default)
    for (String ndcCode : CodeMapUtil.extractNDCsFromCodebase(codeMap)) {
        if (!mapStockVaccineGroupPreviousVersion.containsKey(ndcCode)) {
            mapStockVaccineGroupPreviousVersion.put(ndcCode, false);  // Set non-selected NDCs to false
        }
    }
}


public HashMap<String, Boolean> getMapStockVaccineGroupPreviousVersion() {
    return mapStockVaccineGroupPreviousVersion;
}


public void setMapStockVaccineGroupPreviousVersion(HashMap<String, Boolean> mapStockVaccineGroupPreviousVersion) {
    this.mapStockVaccineGroupPreviousVersion = mapStockVaccineGroupPreviousVersion;
}

public HashMap<String, Boolean> getMapStockVaccineGroup() {
    return mapStockVaccineGroup;
}

public void setMapStockVaccineGroup(HashMap<String, Boolean> mapStockVaccineGroup) {
    this.mapStockVaccineGroup = mapStockVaccineGroup;
}



}
