package org.mitre.synthea.codebase.mapping;

import java.util.*;
import org.mitre.synthea.codebase.*;
import org.mitre.synthea.codebase.generated.*;
import org.mitre.synthea.codebase.generated.Stock.StockMapping;
import org.mitre.synthea.codebase.reference.*;
import org.mitre.synthea.codebase.mapping.*;


import org.cqframework.cql.elm.execution.Code;


/**
 * SpecificCase class is designed to handle specific scenarios 
 * where a CVX (vaccine code) is not directly associated with an NDC (National Drug Code). 
 * These scenarios typically arise when a CVX code has multiple possible mappings, 
 * resembling a tree structure, such as when a CVX is associated with categories like 
 * "adult" and "adolescent," "pediatric" which are in turn linked to specific NDCs.
 */

public class SpecificCase {
    // Create map string (key) - list string (value)
    private Map<String, List<String>> specialCasesMap;
    CodeMap codeMap;

    private Map<String, String> reverseMap;


    public SpecificCase(CodeMap codeMap) {
        this.specialCasesMap = new HashMap<>();
        this.reverseMap = new HashMap<>();
        this.codeMap = codeMap;
        
        // Case Hep B
        List<String> list_45 = new ArrayList<>();
        list_45.add("43"); // adult
        list_45.add("08"); // adolescent, geriatric
        specialCasesMap.put("45", list_45);

        reverseMap.put("43", "45");
        reverseMap.put("08", "45");


        // Case Hep A
        List<String> list_85 = new ArrayList<>();
        list_85.add("52"); // adult
        list_85.add("83"); // Hep A, ped/adol, 2 dose   52 = 83 ?
        // 169 Hep A, live attenuated (not available in the US)
        // 31 Hep A, pediatric, unspecified 
        // 83 Hep A, ped/adol, 2 dose
        specialCasesMap.put("85", list_85);
        reverseMap.put("52", "85");
        reverseMap.put("83", "85");

        // 109: pneumococcal --> PCV * // 100 by default
        List<String> list_109 = new ArrayList<>();
        list_109.add("100"); // poliovirus vaccine, inactivated (IPV)
        list_109.add("100"); // poliovirus vaccine, inactivated (IPV)
        specialCasesMap.put("109", list_109);
        reverseMap.put("100", "109");



        // 89: polio // 10 IPV
        List<String> list_89 = new ArrayList<>();
        list_89.add("10"); // poliovirus vaccine, inactivated (IPV)
        list_89.add("10"); // poliovirus vaccine, inactivated (IPV)
        specialCasesMap.put("89", list_89);
        reverseMap.put("10", "89");


        // 17: Hib, unspecified formulation --> 48 Hib (PRP-T)
        List<String> list_17 = new ArrayList<>();
        list_17.add("48"); // Hib (PRP-T)
        list_17.add("48"); // Hib (PRP-T)
        specialCasesMap.put("17", list_17);
        reverseMap.put("48", "17");

        // 107: DTaP, unspecified formulation --> 20
        List<String> list_107 = new ArrayList<>();
        list_107.add("20"); // diphtheria, tetanus toxoids and acellular pertussis vaccine
        list_107.add("20"); // diphtheria, tetanus toxoids and acellular pertussis vaccine
        specialCasesMap.put("107", list_107);
        reverseMap.put("20", "107");


        // 88: Influenza (random) --> 320 Influenza, MDCK, trivalent, preservative

        List<String> list_88 = new ArrayList<>();
        list_88.add("320");
        list_88.add("320");
        specialCasesMap.put("88", list_88);
        reverseMap.put("320", "88");

        // 122: rotavirus, unspecified formulation --> 119 (random)
        List<String> list_122 = new ArrayList<>();
        list_122.add("119"); // rotavirus, monovalent
        list_122.add("119");
        specialCasesMap.put("122", list_122);
        reverseMap.put("119", "122");

        // 108: meningococcal, unspecified formulation --> 114 (random): meningococcal MCV4P
        List<String> list_108 = new ArrayList<>();
        list_108.add("114"); // meningococcal MCV4P
        list_108.add("114");
        specialCasesMap.put("108", list_108);
        reverseMap.put("114", "108");

        // 137: HPV, unspecified formulation --> 118 (random): HPV, bivalent
        List<String> list_137 = new ArrayList<>();
        list_137.add("118"); // HPV, bivalent
        list_137.add("118");
        specialCasesMap.put("137", list_137);
        reverseMap.put("118", "137");
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


/**
 * Processes a list of CVX codes, removing those that do not generate any NDCs and adding them to a separate list.
 *
 * @param cvxList the list of CVX codes to process
 * @param failedCVXList the list where CVX codes without corresponding NDCs will be added
 */
public void filterCVXList(List<String> cvxList, List<String> failedCVXList) {
    // Create an iterator to modify the list while iterating
    Iterator<String> iterator = cvxList.iterator();

    while (iterator.hasNext()) {
        String cvx = iterator.next();
        // Generate the NDC list for the current CVX
        List<NDC> ndcList = new ArrayList<>();

        Mapping mapping = new Mapping(codeMap);
        ndcList = mapping.createNDCsFromCVXString(Arrays.asList(cvx));

        // If no NDCs are generated, remove from the input list and add to failedCVXList
        if (ndcList.isEmpty()) {
            iterator.remove(); // Remove CVX from the original list
            failedCVXList.add(cvx); // Add CVX to the failed list
        }
    }
}



public List<String> replaceSpecialCases(List<String> inputList) {    
    return replaceSpecialCases(inputList, true);
}


public Map<String, String> getReverseMap() {
    return reverseMap;
}

    
}
