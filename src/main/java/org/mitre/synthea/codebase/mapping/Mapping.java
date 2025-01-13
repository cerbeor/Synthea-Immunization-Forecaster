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

/**
 * The Mapping class is designed to facilitate the creation of NDC (National Drug Code) 
 * objects and their combinations based on related CVX (Vaccination) codes. It provides 
 * methods to generate valid combinations of NDC codes that cover a list of target CVX codes.
 * 
 * Key methods include:
 * - createNDCsFromCVX(List<Code> cvxCodes): Creates NDC objects related to the given list 
 *   of CVX codes, with each NDC containing its associated CVX codes.
 * - createNDCsFromCVXString(List<String> cvxCodes): Similar to the above method but accepts 
 *   a list of CVX codes as strings and converts them into Code objects before creating NDCs.
 * - createCombosFromNDCs(List<NDC> ndcList, List<Code> targetCvxList): Generates valid 
 *   combinations of NDCs to match the target CVX codes.
 * - createCombosFromNDCs(List<NDC> ndcList, List<Code> targetCvxList, int maxDepth, int minCombos): 
 *   Similar to the above method but allows for customization of maximum recursion depth 
 *   and minimum number of combinations to find.
 * 
 * This class implements a backtracking algorithm to find optimal combinations of NDCs that 
 * cover all target CVX codes, with optimization to handle deeper recursions and ensure 
 * uniqueness of combinations.
 */



public class Mapping {
    private CodeMap codeMap;
    // Maximum depth for backtracking recursion
    private static int MAXDEPTH = 10;
    private static int MINCOMBOS = 5;

    // Constructor that takes a CodeMap instance
    public Mapping(CodeMap codeMap) {
        this.codeMap = codeMap;
    }

    /**
     * Creates a list of NDC objects related to a given list of CVX codes.
     * This method maps each CVX code to its corresponding NDC codes, and associates them
     * in the resulting NDC objects. It also tracks any previous code associated with each NDC.
     *
     * For each CVX code in the provided list, the method retrieves the related NDC codes,
     * creates or updates the corresponding NDC objects, and adds the CVX code to the NDC object.
     * Additionally, it adds any previous code if available.
     *
     * @param cvxCodes A list of CVX codes to map to NDC codes.
     * @return A list of NDC objects that are related to the provided CVX codes.
     */

    public List<NDC> createNDCsFromCVX(List<Code> cvxCodes) {
        Map<String, NDC> ndcMap = new HashMap<>();

        for (Code cvxCode : cvxCodes) {
            List<String> relatedNDCs = codeMap.getRelatedValues(cvxCode, CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE);
            
            for (String ndcCode : relatedNDCs) {
                // Create or update the NDC object
                NDC ndc = ndcMap.getOrDefault(ndcCode, new NDC(ndcCode));
                ndc.getCvxCodes().add(cvxCode); // Add the CVX code to the NDC

                // Add previous code if it exists
                ndc.addPreviousCode(cvxCode, codeMap);
                
                ndcMap.put(ndcCode, ndc);
            }
        }

        return new ArrayList<>(ndcMap.values());
    }

    /**
     * Creates a list of NDC objects related to a given list of string CVX codes.
     * This method first converts the list of CVX code strings into `Code` objects,
     * then it maps each CVX code to its corresponding NDC codes, and associates them
     * in the resulting NDC objects.
     *
     * The method calls `createNDCsFromCVX` to handle the actual creation of NDC objects
     * using the converted `Code` objects. It simplifies the process of working with string-based
     * CVX codes by converting them into `Code` objects before proceeding.
     *
     * @param cvxCodes A list of CVX code strings to map to NDC codes.
     * @return A list of NDC objects that are related to the provided CVX codes.
     */

    public List<NDC> createNDCsFromCVXString(List<String> cvxCodes) {
        List<Code> cvxCodeList = new ArrayList<>();
        for (String cvx : cvxCodes) {
            cvxCodeList.add(codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, cvx));
        }
        return createNDCsFromCVX(cvxCodeList);
    }

    /**
     * Creates combinations of NDCs to match the input list of CVX codes.
     * This method serves as a wrapper for the more flexible version of the method
     * `createCombosFromNDCs` that allows specifying a maximum recursion depth and
     * a minimum number of combinations to be found. The default values for maximum
     * depth and minimum combinations are used.
     *
     * The method attempts to find valid combinations of NDC codes that cover the given
     * list of CVX codes. The combination process optimizes for fewer NDCs while ensuring
     * all target CVX codes are covered.
     *
     * @param ndcList The list of NDC objects to search for combinations.
     * @param targetCvxList The list of CVX codes to match in the combinations.
     * @return A list of valid combinations of NDCs that cover the provided CVX codes.
     */
    public List<Combo> createCombosFromNDCs(List<NDC> ndcList, List<Code> targetCvxList){
        return createCombosFromNDCs(ndcList, targetCvxList, MAXDEPTH, MINCOMBOS);
    }


    /**
     * Creates combinations of NDCs to match the input list of CVX codes.
     * 
     * @param ndcList List of NDC objects to search for combinations.
     * @param targetCvxList List of CVX codes to cover in the combinations.
     * @param maxDepth Maximum recursion depth to avoid excessive computation.
     * @param minCombos Minimum number of combinations to find before stopping the search.
     * @return List of Combo objects representing valid combinations of NDCs.
     */
    public List<Combo> createCombosFromNDCs(List<NDC> ndcList, List<Code> targetCvxList, int maxDepth, int minCombos) {
        List<Combo> combos = new ArrayList<>();
        
        // Group NDCs by their CVX codes for efficient searching
        Map<Set<Code>, List<NDC>> groupedNDCs = new HashMap<>();
        for (NDC ndc : ndcList) {
            Set<Code> cvxSet = new HashSet<>(ndc.getCvxCodes());
            groupedNDCs.computeIfAbsent(cvxSet, k -> new ArrayList<>()).add(ndc);
        }

        // Use backtracking to find the best combinations
        findAndFilterCombos(new ArrayList<>(), targetCvxList, groupedNDCs, combos, new HashSet<>(), maxDepth, 0, minCombos);
        // Sort by the number of NDCs (fewer NDCs is better)
        combos.sort(Comparator.comparingInt(c -> c.getNdcList().size()));

        return combos;
    }

    /**
     * Helper method to recursively find combinations of NDCs to match the target CVX list
     * while removing duplicates during the process.
     * 
     * @param currentCombo Current list of NDCs being evaluated as a potential combination.
     * @param remainingCvx List of CVX codes that still need to be covered.
     * @param groupedNDCs Map where the key is a set of CVX codes and the value is a list of NDCs covering those codes.
     * @param combos List of valid combinations found.
     * @param uniqueCombos A set of unique combinations to ensure no duplicates.
     * @param maxDepth Maximum recursion depth to avoid excessive computation.
     * @param currentDepth Current depth of the recursion.
     * @param minCombos Minimum number of combinations to find before stopping the search.
     */
    private void findAndFilterCombos(List<NDC> currentCombo, List<Code> remainingCvx, 
                                     Map<Set<Code>, List<NDC>> groupedNDCs, List<Combo> combos, 
                                     Set<Set<String>> uniqueCombos, int maxDepth, int currentDepth, int minCombos) {
        // Base case 1: If no CVX codes remain to be covered, process the current combination.
        if (remainingCvx.isEmpty()) {
            Set<String> ndcCodesSet = currentCombo.stream()
                                                  .map(NDC::getNdcCode)
                                                  .collect(Collectors.toSet());

            if (uniqueCombos.add(ndcCodesSet)) {
                combos.add(new Combo(new ArrayList<>(currentCombo), true, 100.0));
            }
            return;
        }

        // Base case 2: If maximum depth is reached, exit recursion.
        if (currentDepth >= maxDepth) {
            return;
        }

        // Stop recursion if the minimum number of combinations is reached.
        if (combos.size() >= minCombos) {
            return;
        }

        // Heuristic: Sort the remaining CVX codes by their coverage frequency to optimize for rare codes first.
        remainingCvx.sort((a, b) -> {
            long countA = groupedNDCs.entrySet().stream()
                    .filter(entry -> entry.getKey().contains(a))
                    .count();
            long countB = groupedNDCs.entrySet().stream()
                    .filter(entry -> entry.getKey().contains(b))
                    .count();
            return Long.compare(countA, countB);
        });

        // Iterate through each entry in the grouped NDCs.
        for (Map.Entry<Set<Code>, List<NDC>> entry : groupedNDCs.entrySet()) {
            Set<Code> ndcCvxSet = entry.getKey();
            List<NDC> ndcGroup = entry.getValue();

            // Skip if any NDC in the current combo overlaps with this NDC group.
            if (currentCombo.stream().anyMatch(ndc -> !Collections.disjoint(ndc.getCvxCodes(), ndcCvxSet))) {
                continue;
            }

            // Identify CVX codes in the remaining list that intersect with this NDC group.
            List<Code> intersectingCvx = remainingCvx.stream()
                    .filter(ndcCvxSet::contains)
                    .collect(Collectors.toList());

            // If there are intersecting CVX codes, recursively process them.
            if (!intersectingCvx.isEmpty()) {
                for (NDC ndc : ndcGroup) {
                    currentCombo.add(ndc); // Add current NDC to the combo.
                    List<Code> newRemaining = new ArrayList<>(remainingCvx);
                    newRemaining.removeAll(ndc.getCvxCodes()); // Remove covered CVX codes.

                    findAndFilterCombos(currentCombo, newRemaining, groupedNDCs, combos, uniqueCombos, maxDepth, currentDepth + 1, minCombos);

                    currentCombo.remove(currentCombo.size() - 1); // Backtrack by removing the last NDC.
                }
            }
        }
    }
}
