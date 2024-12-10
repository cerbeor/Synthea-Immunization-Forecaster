package org.mitre.synthea.codebase.mapping;

import java.util.*;


/**
 * The {@code SpecificCase} class is designed to handle specific scenarios 
 * where a CVX (vaccine code) is not directly associated with an NDC (National Drug Code). 
 * These scenarios typically arise when a CVX code has multiple possible mappings, 
 * resembling a tree structure, such as when a CVX is associated with categories like 
 * "adult" and "adolescent," "pediatric" which are in turn linked to specific NDCs.
 */

public class SpecificCase {
    // Create map string (key) - list string (value)
    private Map<String, List<String>> specialCasesMap;

    public SpecificCase() {
        this.specialCasesMap = new HashMap<>();
        
        // Case Hep B
        List<String> list45 = new ArrayList<>();
        list45.add("43");
        list45.add("08");
        specialCasesMap.put("45", list45);

    }


    /**
 * Processes a list of strings, replacing special cases with a unique case based on the isAdult parameter.
 *
 * @param inputList the list of strings to process
 * @param isAdult a boolean indicating whether to select the "adult" case (true) or "adolescent" case (false)
 * @return a new list of strings with special cases replaced by the corresponding unique case
 */
public List<String> replaceSpecialCases(List<String> inputList, boolean isAdult) {    
    // Create a new list to store the processed values
    List<String> outputList = new ArrayList<>();
    
    // Iterate over the input list
    for (String value : inputList) {
        // Check if the value is a special case
        if (specialCasesMap.containsKey(value)) {
            // Replace with the unique case based on isAdult
            List<String> possibilities = specialCasesMap.get(value);
            outputList.add(isAdult ? possibilities.get(0) : possibilities.get(1));
        } else {
            // Keep the value as is if it's not a special case
            outputList.add(value);
        }
    }
    return outputList;
}


public List<String> replaceSpecialCases(List<String> inputList) {    
    return replaceSpecialCases(inputList, true);
}

    
}
