package org.mitre.synthea.codebase.generated.Stock;

import java.util.*;
import java.util.stream.Collectors;

import org.mitre.synthea.codebase.*;
import org.mitre.synthea.codebase.mapping.*;
import org.mitre.synthea.codebase.reference.CodesetType;
import org.mitre.synthea.codebase.generated.*;;

/**
 * The StockMapping class is responsible for managing the mapping between vaccine groups, NDC codes,
 * and stock availability. It provides functionality to:
 * 
 * 1. **Initialize the stock map** for vaccine groups, with randomly selected NDCs marked as in-stock.
 * 2. **Map CVX codes to associated vaccine groups and NDCs**, linking each CVX to relevant NDC codes.
 * 3. **Find NDCs not associated with any vaccine group**, and generate statistics regarding the mappings.
 * 4. **Generate NDC combinations** that cover the required vaccine groups, considering multiple combinations
 *    and filtering based on vaccine group coverage.
 * 5. **Track and update stock status** for the selected vaccine group and NDC combinations.
 * 
 * The class supports:
 * - Random selection of NDCs for stock initialization,
 * - Retrieval and mapping of NDCs based on vaccine group and CVX code relationships,
 * - Generation of NDC combinations that meet a required vaccine group coverage,
 * - Detailed statistics about the NDC-vaccine group mappings, including those NDCs not assigned to any vaccine group.
 *
 * This class helps simulate vaccine stock management for different vaccine groups and their corresponding NDCs.
 *
 * Example Usage:
 * 
 * StockMapping stockMapping = new StockMapping(codeMap);
 * Map<String, List<NDC>> vaccineGroupMap = stockMapping.mapCvxToVaccineGroupsAndNDCs();
 * stockMapping.initializeAndSelectRandomNDCs(vaccineGroupMap, codeMap, 5);
 * 
 * 
 * Methods:
 * - initializeAndSelectRandomNDCs: Initializes the stock map by selecting random NDCs for each vaccine group.
 * - findNDCsWithoutVaccineGroup: Finds and returns a list of NDCs not linked to any vaccine group.
 * - mapCvxToVaccineGroupsAndNDCs: Maps CVX codes to their associated vaccine groups and NDCs.
 * - generateNdcCombinations: Generates combinations of NDCs that cover all required vaccine groups.
 * - initializeStockVaccineGroup: Initializes the stock status for vaccine group combinations.
 *
 * The main goal of this class is to simulate vaccine availability across different groups and assist in
 * managing stock status based on combinations of NDC codes.
 */



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
 * Initializes the stock map for vaccine groups by selecting a random set of NDCs and marking them as in-stock.
 * 
 * This method sets up the `mapStockVaccineGroup` with all NDCs marked as false (out of stock) initially.
 * Then, for each vaccine group, it randomly selects up to 'n' NDCs and marks them as true (in stock).
 * 
 * @param vaccineGroupMap A map where the key is the vaccine group name (String) and the value is a list of NDC objects.
 *                        Each vaccine group contains a list of NDCs that represent the available vaccine products for that group.
 * @param codeMap The `CodeMap` instance which contains all the NDCs available in the codebase.
 *                This is used to extract all the NDC codes and manage the stock for each.
 * @param n The number of NDCs to randomly select from each vaccine group. If the number of NDCs in a group is less than 'n',
 *          all NDCs in that group will be selected.
 * 
 * @return void This method updates the `mapStockVaccineGroup` with true for the selected NDCs and false for all others.
 * 
 * @see mapStockVaccineGroup
 * @see CodeMapUtil#extractNDCsFromCodebase(CodeMap)
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
 * Finds all NDCs that are not associated with any vaccine group in the provided vaccineGroupMap.
 * 
 * This method performs the following steps:
 * 1. Extracts all NDC codes from the given CodeMap.
 * 2. Collects all NDC codes that are linked to vaccine groups in the vaccineGroupMap.
 * 3. Identifies the NDC codes that are not present in any vaccine group and returns them.
 * 4. Prints statistics on the total number of NDCs, the number of linked and unlinked NDCs, and the count of vaccine groups with or without NDCs.
 * 
 * @param vaccineGroupMap A map where the key is a vaccine group name (String) and the value is a list of NDC objects.
 *                        Each vaccine group contains a list of NDCs representing the available vaccine products for that group.
 * @param codeMap The `CodeMap` instance which contains all the NDCs available in the codebase. This is used to extract the NDC codes.
 * 
 * @return A list of strings representing NDC codes that are not associated with any vaccine group.
 * 
 * @see CodeMapUtil#extractNDCsFromCodebase(CodeMap)
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
 * Maps CVX codes to vaccine groups and associated NDCs.
 * 
 * This method performs the following steps:
 * 1. Retrieves all CVX codes from the provided `codeMap`.
 * 2. For each CVX code, it retrieves the associated NDC codes and vaccine groups.
 * 3. Maps each vaccine group to a list of NDCs, and associates the relevant CVX code with each NDC.
 * 4. Returns a map where each key is a vaccine group and the value is a list of NDCs associated with that group.
 * 
 * @return A map where the key is a vaccine group (String) and the value is a list of NDC objects (List<NDC>) 
 *         associated with that vaccine group.
 * 
 * @see CodeMapUtil#extractCvxFromCodebase(CodeMap)
 * @see CodeMap#getCodeForCodeset(CodesetType, String)
 * @see CodeMap#getRelatedValues(Code, CodesetType)
 * @see StockMapping#getVaccineGroupLabelsFromCvx(String)
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
 * Retrieves the vaccine group labels associated with a given CVX code.
 * 
 * This method performs the following steps:
 * 1. Uses the `RelatedCode` class to fetch vaccine group codes associated with the provided CVX code.
 * 2. Adds the found vaccine group labels to a list.
 * 3. Returns a list of vaccine group labels associated with the CVX.
 * 
 * @param cvx The CVX code for which to retrieve associated vaccine group labels.
 * @return A list of vaccine group labels (String) associated with the given CVX code. 
 *         Returns an empty list if no groups are found, or `null` if there are no related values.
 * 
 * @see RelatedCode#getVaccineGroupLabelsFromCvx(String)
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






// Previous version: The combinatorial approach generated all possible NDC combinations, but it was too slow and inefficient for large datasets.




 /**
 * Generates all possible combinations of NDCs and evaluates their coverage of vaccine groups.
 * 
 * This method performs the following steps:
 * 1. Initializes the list of vaccine groups and NDCs from the codebase.
 * 2. Associates each NDC with its related vaccine groups using CVX codes.
 * 3. Generates all possible combinations of NDCs.
 * 4. Sorts the combinations by their effectiveness, prioritizing those that cover all vaccine groups and
 *    use fewer NDCs.
 * 5. Filters the combinations, returning only those that fully cover all vaccine groups if they exist.
 * 6. Returns the filtered list of combinations or all combinations if no fully covered ones exist.
 * 
 * @param n The maximum number of NDCs allowed in a combination.
 * @param m The minimum number of vaccine groups that must be covered in each combination.
 * @return A list of `Combo` objects representing the best NDC combinations. If combinations that cover all 
 *         vaccine groups are found, only those are returned. Otherwise, all combinations are returned.
 * 
 * @see RelatedCode#getCvxCodesFromNdc(String)
 * @see RelatedCode#getVaccineGroupLabelsFromCvx(String)
 * @see Combo#getScore()
 * @see Combo#isAllVaccineGroupsFound()
 */
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

/**
 * Recursively generates combinations of NDCs to cover the target vaccine groups at least 'n' times.
 * The method explores all possible combinations of NDCs, calculates the coverage of each group, 
 * and stops early if the desired number of combinations (m) is reached or if a combination 
 * fully covers all target groups.
 *
 * @param currentCombo        The current combination of NDCs being explored.
 * @param remainingNdcList    The list of remaining NDCs to be considered for the combination.
 * @param targetGroups        The list of target vaccine groups that need to be covered.
 * @param n                   The minimum number of times each target group must be covered.
 * @param m                   The maximum number of combinations to generate. If -1, there is no limit.
 * @param resultCombos        The list to store the generated combinations that meet the criteria.
 * @param ndcToVaccineGroups  A map that associates each NDC with its covered vaccine groups.
 */

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


/**
 * Initializes the stock vaccine group by selecting a combination of NDCs and updating the map of vaccine group statuses.
 * Depending on the 'random' flag, a combination of NDCs is selected either randomly or as the first combination from the list.
 * The NDCs in the selected combination are marked as true in the stock vaccine group map, while others are marked as false.
 *
 * @param combos    A list of Combo objects representing different combinations of NDCs.
 * @param random    A flag that determines whether to select a random combo (true) or the first combo (false).
 */

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
