package org.mitre.synthea.codebase.mapping;

import java.util.ArrayList;
import java.util.List;

import org.mitre.synthea.codebase.CodeMap;
import org.mitre.synthea.codebase.generated.*;
import org.mitre.synthea.codebase.reference.CodesetType;

/**
 * The NDC class represents a National Drug Code (NDC) and its associated information.
 * This class contains the NDC code itself, the related CVX (Vaccination) codes, and vaccine groups 
 * that are associated with the NDC. It also keeps track of previous codes related to the NDC, 
 * which may be important for historical mapping or specific cases.
 * 
 * Key fields include:
 * - ndcCode: The actual National Drug Code (NDC) string.
 * - cvxCodes: A list of CVX codes (related to vaccination) associated with this NDC.
 * - vaccineGroups: A list of vaccine group codes associated with the NDC.
 * - previousCodes: A list of previous codes associated with this NDC, often used for historical mapping or reverse code lookups.
 * 
 * Key methods include:
 * - getCvxCodes() / setCvxCodes(): Getters and setters for the CVX codes related to this NDC.
 * - getNdcCode() / setNdcCode(): Getter and setter for the NDC code.
 * - getVaccineGroups() / setVaccineGroups(): Getters and setters for the vaccine group codes associated with this NDC.
 * - toString(): Provides a string representation of the NDC, including its CVX codes.
 * - hashCode() / equals(): Methods to ensure that NDC objects can be used properly in collections like sets or maps.
 * - getPreviousCodes(): Getter for the list of previous codes associated with the NDC.
 * - addPreviousCode(): Method to add a previous code (e.g., reversing CVX codes) to the NDC's previousCodes list.
 * - reverseCode(): Method that takes a CVX code and reverses it based on a specific mapping.
 * 
 * The NDC class is an important part of the mapping system in the Synthea codebase, enabling 
 * efficient tracking and management of NDC codes, CVX codes, vaccine groups, and historical code relationships.
 */



public class NDC {
    private String ndcCode;
    private List<Code> cvxCodes;
    private List<Code> vaccineGroups; // ArrayList for Vaccine Groups

    // List of previous codes (before specific cases)
    private List<String> previousCodes;

    public NDC(String ndcCode) {
        this.ndcCode = ndcCode;
        this.cvxCodes =  new ArrayList<>();
        this.vaccineGroups = new ArrayList<>();

        previousCodes = new ArrayList<>();
    }

    public List<Code> getCvxCodes() {
        return cvxCodes;
    }

    public void setCvxCodes(List<Code> cvxCodes) {
        this.cvxCodes = cvxCodes;
    }

    public String getNdcCode() {
        return ndcCode;
    }

    public void setNdcCode(String ndcCode) {
        this.ndcCode = ndcCode;
    }

    public List<Code> getVaccineGroups() {
        return vaccineGroups;
    }

    public void setVaccineGroups(List<Code> vaccineGroups) {
        this.vaccineGroups = vaccineGroups;
    }

    @Override
    public String toString() {  
        String result = "NDC: " + ndcCode +" :\n";
        for(Code cvxCode : cvxCodes) {
            result += ", CVX Code: " + cvxCode.getLabel();
        }
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ndcCode == null) ? 0 : ndcCode.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NDC other = (NDC) obj;
        if (ndcCode == null) {
            if (other.ndcCode != null)
                return false;
        } else if (!ndcCode.equals(other.ndcCode))
            return false;
        return true;
    }

    public List<String> getPreviousCodes() {
        return previousCodes;
    }


    // adding to previous codes
    public void addPreviousCode(Code code, CodeMap codeMap) {
        // cvx code to string
        this.previousCodes.add(this.reverseCode(code, codeMap));
    }


    // function reverse code
    public String reverseCode(Code cvxCode, CodeMap codeMap) {
        // cvx code to string
        String cvxCodeString = codeMap.getStringForCode(cvxCode, CodesetType.VACCINATION_CVX_CODE);
        // reverse CVX
        SpecificCase specificCase = new SpecificCase(codeMap);
        if(specificCase.getReverseMap().containsKey(cvxCodeString)) {
            cvxCodeString = specificCase.getReverseMap().get(cvxCodeString);
        }
        // reverse CVX
        return cvxCodeString;
    }

    
}