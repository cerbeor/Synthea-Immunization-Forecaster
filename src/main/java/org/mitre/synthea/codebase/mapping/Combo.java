package org.mitre.synthea.codebase.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Combo {
    private List<NDC> ndcList; // List of NDCs that form this Combo
    private double score; // Score of the Combo based on the relevance
    private boolean allCvxFound; // Indicates if all CVX codes were matched
    private double completionPercentage; // Percentage of CVX codes covered if incomplete

    private boolean allVaccineGroupsFound; // Indicates if all vaccine groups were matched

    public Combo(List<NDC> ndcList, boolean allCvxFound, double completionPercentage) {
        this.ndcList = new ArrayList<>(ndcList);
        this.allCvxFound = allCvxFound;
        this.completionPercentage = completionPercentage;
        this.score = calculateScore();
    }

    

    // Getters
    public List<NDC> getNdcList() {
        return Collections.unmodifiableList(ndcList);
    }

    public double getScore() {
        return score;
    }

    public boolean isAllCvxFound() {
        return allCvxFound;
    }

    public double getCompletionPercentage() {
        return completionPercentage;
    }

    // Calculate the score based on the number of NDCs and completion
    private double calculateScore() {
        // Higher score if fewer NDCs are used, and if all CVX codes are found
        double baseScore = 100;
        if (!allCvxFound) {
            baseScore *= completionPercentage;
        }
        return baseScore / ndcList.size();
    }

    @Override
    public String toString() {
        return String.format("\n Combo :\n[NDCs: %s, Score: %.2f\n, All CVX Found: %b\n, Completion: %.2f%%\n, All Vaccine Groups Found: %b]",
                             ndcList, score, allCvxFound, completionPercentage, allVaccineGroupsFound);
    }



    public boolean isAllVaccineGroupsFound() {
        return allVaccineGroupsFound;
    }



    public void setAllVaccineGroupsFound(boolean allVaccineGroupsFound) {
        this.allVaccineGroupsFound = allVaccineGroupsFound;
    }

    
}