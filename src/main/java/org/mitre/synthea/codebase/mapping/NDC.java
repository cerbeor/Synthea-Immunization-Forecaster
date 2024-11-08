package org.mitre.synthea.codebase.mapping;

import java.util.ArrayList;
import java.util.List;
import org.mitre.synthea.codebase.generated.*;


public class NDC {
    private String ndcCode;
    private List<Code> cvxCodes; // Assuming CVXCode is the type of items in the list

    // Constructor that matches the call in your code
    public NDC(String ndcCode) {
        this.ndcCode = ndcCode;
        this.cvxCodes =  new ArrayList<>();
    }

    // Getter for CVX codes list, if needed
    public List<Code> getCvxCodes() {
        return cvxCodes;
    }

    // Setter for CVX codes list, if needed
    public void setCvxCodes(List<Code> cvxCodes) {
        this.cvxCodes = cvxCodes;
    }

    // Additional getters and setters for other fields, if needed
    public String getNdcCode() {
        return ndcCode;
    }

    public void setNdcCode(String ndcCode) {
        this.ndcCode = ndcCode;
    }

    @Override
    public String toString() {  
        String result = "NDC: " + ndcCode +" :\n";
        for(Code cvxCode : cvxCodes) {
            result += ", CVX Code: " + cvxCode.getLabel();
        }
        return result;
    }
}