package org.mitre.synthea.codebase.mapping;
import java.util.*;


/**
 * The VaccineGroup class represents a vaccine group and its associated list of NDCs (National Drug Codes).
 * Each vaccine group is identified by a unique group name and has a list of NDCs that are associated with it.
 * This class provides methods for managing the vaccine group name and the NDC list, 
 * including adding NDCs to the list and accessing the group's information.
 * 
 * The class also includes getter and setter methods for both the group name and the NDC list.
 */

public class VaccineGroup {
    private String groupName;
    private List<NDC> ndcList;

    public VaccineGroup(String groupName) {
        this.groupName = groupName;
        this.ndcList = new ArrayList<>();
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<NDC> getNdcList() {
        return ndcList;
    }

    public void setNdcList(List<NDC> ndcList) {
        this.ndcList = ndcList;
    }

    public void addNdc(NDC ndc) {
        this.ndcList.add(ndc);
    }

    @Override
    public String toString() {
        return "VaccineGroup{" +
                "groupName='" + groupName + '\'' +
                ", ndcList=" + ndcList +
                '}';
    }
}
