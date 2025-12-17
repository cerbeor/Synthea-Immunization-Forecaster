import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mitre.synthea.codebase.*;
import org.mitre.synthea.codebase.generated.*;
import org.mitre.synthea.codebase.mapping.Combo;
import org.mitre.synthea.codebase.reference.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StockTest {


    private static CodeMap codeMap;


    private static Code cvxCodeObj1;
    private static Code cvxCodeObj2;
    private static Code cvxCodeObj3;

    // Combo test

    private static Code cvxCodeObj4;
    private static Code cvxCodeObj5;
    private static Code cvxCodeObj6;


    @BeforeClass
    public static void setUp() {
        codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap("./src/test/resources/CompiledTest.xml");
        // Retrieve Code objects for each CVX
        cvxCodeObj1 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "001");
        cvxCodeObj2 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "002");
        cvxCodeObj3 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "003");

        // Combo test

        cvxCodeObj4 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "004");
        cvxCodeObj5 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "005");
        cvxCodeObj6 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "006");
    }

    @Test
    public void testExtractNDCsFromCodebase() {
        CodeMapUtil codeMapUtil = new CodeMapUtil();
        List<String> ndcList = codeMapUtil.extractNDCsFromCodebase(codeMap);

        assertNotNull("The NDC list should not be null", ndcList);
        assertEquals(7, ndcList.size());
    }

}
