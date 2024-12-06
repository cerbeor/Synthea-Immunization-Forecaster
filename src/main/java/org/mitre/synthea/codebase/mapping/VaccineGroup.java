package org.mitre.synthea.codebase.mapping;
import java.util.*;

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
