package org.mitre.synthea.codebase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mitre.synthea.codebase.generated.Code;
import org.mitre.synthea.codebase.reference.CodesetType;

public class CodeMapUtil {

    /**
     * Extracts all NDC (National Drug Code) values from the given CodeMap instance.
     * 
     * This method focuses on NDC-related CodesetTypes, including:
     * - VACCINATION_NDC_CODE
     * - VACCINATION_NDC_CODE_UNIT_OF_USE
     * - VACCINATION_NDC_CODE_UNIT_OF_SALE
     * 
     * @param codeMap The CodeMap instance containing various codesets.
     * @return A list of NDC values (strings) extracted from the relevant codesets.
     */
    public static List<String> extractNDCsFromCodebase(CodeMap codeMap) {
        // Utilisation d'un Set pour éviter les doublons
        Set<String> ndcSet = new HashSet<>();
        
        // Récupérer toute la map CodeBase à partir de l'instance CodeMap
        Map<CodesetType, Map<String, Code>> codeBaseMap = codeMap.getCodeBaseMap();
        
        if (codeBaseMap == null || codeBaseMap.isEmpty()) {
            return new ArrayList<>(); // Retourne une liste vide si aucun code n'est trouvé
        }
        
        // Définir les types de Codeset NDC auxquels nous nous intéressons
        CodesetType[] ndcCodesetTypes = {
            CodesetType.VACCINATION_NDC_CODE,
            CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE,
            CodesetType.VACCINATION_NDC_CODE_UNIT_OF_SALE
        };
        
        // Parcourir chaque CodesetType lié aux NDC et collecter les valeurs NDC
        for (CodesetType codesetType : ndcCodesetTypes) {
            Map<String, Code> ndcCodeset = codeBaseMap.get(codesetType);
            
            if (ndcCodeset != null) {
                // Extraire toutes les valeurs NDC du type de codeset actuel
                for (Map.Entry<String, Code> entry : ndcCodeset.entrySet()) {
                    String ndcValue = entry.getKey(); // NDC Code
                    ndcSet.add(ndcValue); // Ajout au Set (pas de doublons)
                }
            }
        }
        
        // Convertir le Set en List avant de le retourner
        return new ArrayList<>(ndcSet);
    }
}
