package org.mitre.synthea.codebase.mapping;

import java.util.ArrayList;
import java.util.List;


/**
 * The CVX class represents a CVX (Clinical Vaccine Code) and its associated vaccine groups.
 * It holds a CVX code and a list of vaccine groups that are linked to that specific CVX code.
 * This class provides methods to manage the CVX code and the associated vaccine groups, 
 * including adding and removing vaccine groups, and accessing the information.
 * 
 * The class also offers getter and setter methods for the CVX code and vaccine groups,
 * ensuring that the list of vaccine groups is never null.
 */

public class CVX {
    private String code; // The CVX code
    private List<String> vaccineGroups; // List of vaccine groups associated with this CVX

    // Constructor
    public CVX(String code, List<String> vaccineGroups) {
        this.code = code;
        this.vaccineGroups = vaccineGroups != null ? vaccineGroups : new ArrayList<>();
    }

    // Getter for the CVX code
    public String getCode() {
        return code;
    }

    // Setter for the CVX code
    public void setCode(String code) {
        this.code = code;
    }

    // Getter for the list of vaccine groups
    public List<String> getVaccineGroups() {
        return vaccineGroups;
    }

    // Setter for the list of vaccine groups
    public void setVaccineGroups(List<String> vaccineGroups) {
        this.vaccineGroups = vaccineGroups != null ? vaccineGroups : new ArrayList<>();
    }

    // Adds a single vaccine group to the list
    public void addVaccineGroup(String vaccineGroup) {
        if (vaccineGroup != null && !vaccineGroups.contains(vaccineGroup)) {
            vaccineGroups.add(vaccineGroup);
        }
    }

    // Removes a vaccine group from the list
    public void removeVaccineGroup(String vaccineGroup) {
        vaccineGroups.remove(vaccineGroup);
    }

    @Override
    public String toString() {
        return "CVX{" +
                "code='" + code + '\'' +
                ", vaccineGroups=" + vaccineGroups +
                '}';
    }
}
