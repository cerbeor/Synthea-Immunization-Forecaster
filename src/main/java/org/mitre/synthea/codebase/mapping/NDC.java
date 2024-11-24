package org.mitre.synthea.codebase.mapping;

import java.util.ArrayList;
import java.util.List;
import org.mitre.synthea.codebase.generated.*;


public class NDC {
    private String ndcCode;
    private List<Code> cvxCodes;
    private List<Code> vaccineGroups; // ArrayList for Vaccine Groups

    public NDC(String ndcCode) {
        this.ndcCode = ndcCode;
        this.cvxCodes =  new ArrayList<>();
        this.vaccineGroups = new ArrayList<>();
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

    
}