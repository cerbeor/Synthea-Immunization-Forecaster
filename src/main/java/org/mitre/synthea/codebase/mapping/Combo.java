package org.mitre.synthea.codebase.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Combo class represents a combination of NDC (National Drug Code) objects that together 
 * aim to cover a specific set of CVX (Vaccination) codes or vaccine groups. This class 
 * is used to evaluate and score different combinations of NDCs based on how well they 
 * cover the target CVX codes or vaccine groups.
 * 
 * Key fields include:
 * - ndcList: A list of NDC objects that form this combination.
 * - score: A calculated score representing the relevance or efficiency of this combination.
 * - allCvxFound: A boolean indicating whether all the CVX codes were matched in the combination.
 * - completionPercentage: The percentage of CVX codes covered by this combination if incomplete.
 * - scoreVaccineGroup: The score related to covering vaccine groups.
 * - allVaccineGroupsFound: A boolean indicating whether all the vaccine groups were matched.
 * - vaccineGroupCompletionPercentage: The percentage of vaccine groups covered if incomplete.
 * 
 * Key methods include:
 * - calculateScore(): Calculates the score of the combination based on the number of NDCs used 
 *   and the completion of CVX codes.
 * - calculateScoreVaccineGroup(): Similar to the above method but based on the completion of vaccine groups.
 * - toString(): Returns a string representation of the Combo, including the list of NDCs, the score, 
 *   whether all CVX codes are found, the completion percentage, and vaccine group status.
 * 
 * This class is used in combination with other classes, such as Mapping, to evaluate optimal combinations 
 * of NDCs that match target CVX codes or vaccine groups.
 */

public class Combo {
    private List<NDC> ndcList; // List of NDCs that form this Combo
    private double score; // Score of the Combo based on the relevance
    private boolean allCvxFound; // Indicates if all CVX codes were matched
    private double completionPercentage; // Percentage of CVX codes covered if incomplete

    private double scoreVaccineGroup; 
    private boolean allVaccineGroupsFound; // Indicates if all vaccine groups were matched
    private double vaccineGroupCompletionPercentage;

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

    /**
     * Calculates the score for a given combination of NDCs based on the number of NDCs used and the completion of CVX codes.
     * The score is designed to reward combinations with fewer NDCs and those that cover more of the required CVX codes.
     *
     * The score is computed as follows:
     * - A base score of 100 is given.
     * - If not all CVX codes are found, the score is adjusted by the completion percentage (percentage of CVX codes covered).
     * - The final score is then divided by the number of NDCs in the combination, with fewer NDCs resulting in a higher score.
     *
     * @return The calculated score for the combination of NDCs.
     */

    private double calculateScore() {
        // Higher score if fewer NDCs are used, and if all CVX codes are found
        double baseScore = 100;
        if (!allCvxFound) {
            baseScore *= completionPercentage;
        }
        return baseScore / ndcList.size();
    }

    /**
     * Calculates the score for a given combination of NDCs based on the number of vaccine groups covered and the completion of these groups.
     * The score is designed to reward combinations that use fewer NDCs and those that cover more of the required vaccine groups.
     *
     * The score is computed as follows:
     * - A base score of 100 is given.
     * - If not all vaccine groups are found, the score is adjusted by the vaccine group completion percentage.
     * - The final score is then divided by the number of NDCs in the combination, with fewer NDCs resulting in a higher score.
     *
     * @return The calculated score for the combination of NDCs, based on vaccine group completion.
     */

    public double calculateScoreVaccineGroup() {
        // Higher score if fewer NDCs are used, and if all CVX codes are found
        double baseScore = 100;
        if (!allVaccineGroupsFound) {
            baseScore *= vaccineGroupCompletionPercentage;
        }
        scoreVaccineGroup = baseScore / ndcList.size();
        return scoreVaccineGroup;
    }


    /**
     * Returns a string representation of the Combo object, including key details such as the list of NDCs,
     * the overall score, whether all CVX codes were found, the completion percentage, and whether all vaccine groups
     * were matched.
     *
     * The string format is as follows:
     * - NDCs: List of NDCs in the combination
     * - Score: The calculated score for the combination
     * - All CVX Found: A boolean indicating if all CVX codes were found
     * - Completion: The percentage of CVX codes covered
     * - All Vaccine Groups Found: A boolean indicating if all vaccine groups were matched
     *
     * @return A formatted string summarizing the details of the Combo.
     */

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



    public double getVaccineGroupCompletionPercentage() {
        return vaccineGroupCompletionPercentage;
    }



    public void setVaccineGroupCompletionPercentage(double vaccineGroupCompletionPercentage) {
        this.vaccineGroupCompletionPercentage = vaccineGroupCompletionPercentage;
    }

    
}