
import java.util.*;

import org.mitre.synthea.codebase.*;
import org.mitre.synthea.codebase.generated.*;
import org.mitre.synthea.codebase.reference.*;
import org.mitre.synthea.codebase.mapping.*;

public class MainTest {
    
    public static void main(String[] args) {
        CodeMap codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap("./src/test/resources/compiledTest/CompiledTestCombo.xml");


        // String ndcCode = "00001-0000-01";  // Example of an existing NDC

        // String drugDetails = CodeMap.getDrugDetailsByNDC(ndcCode);
        // System.out.println(drugDetails);


        // Code cvxCodeObj4 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "004");
        // Code cvxCodeObj5 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "005");
        // Code cvxCodeObj6 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "006");

        // List<Combo> comboList = codeMap.getCombosByCVXList(Arrays.asList(cvxCodeObj4, cvxCodeObj5, cvxCodeObj6));
        // System.out.println("\n");
        // System.out.println(comboList);

        // System.out.println("Taille liste 1 - "+comboList.get(0).getNdcList().size());
        // System.out.println("Taille liste 2 - "+comboList.get(1).getNdcList().size());


        List<String> ndcList = CodeMapUtil.extractNDCsFromCodebase(codeMap);

        // show us NDC list
        System.out.println(ndcList);
        System.out.println(ndcList.size());
    }
}
