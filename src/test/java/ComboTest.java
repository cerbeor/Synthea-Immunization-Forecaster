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
public class ComboTest {


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
        codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap("./src/test/resources/CompiledTest/CompiledTestCombo.xml");
        // Retrieve Code objects for each CVX
        cvxCodeObj1 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "001");
        cvxCodeObj2 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "002");
        cvxCodeObj3 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "003");

        // Combo test

        cvxCodeObj4 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "004");
        cvxCodeObj5 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "005");
        cvxCodeObj6 = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "006");
    }

    // init test
    @Test
    public void testCreateNDCsFromCVX() {
        // Verify that CVX codes exist
        assertNotNull("The CVX code 001 should exist in the CodeMap", cvxCodeObj1);
        assertNotNull("The CVX code 002 should exist in the CodeMap", cvxCodeObj2);
        assertNotNull("The CVX code 003 should exist in the CodeMap", cvxCodeObj3);

        assertNotNull("The CVX code 004 should exist in the CodeMap", cvxCodeObj4);
        assertNotNull("The CVX code 005 should exist in the CodeMap", cvxCodeObj5);
        assertNotNull("The CVX code 006 should exist in the CodeMap", cvxCodeObj6);
    }

    @Test
    public void testGetDrugDetailsByNDC_ExistingNDC() {
        String ndcCode = "00001-0000-01";  // Example of an existing NDC
        Code ndcCodeObj = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE, ndcCode);
        
        assertNotNull("The NDC code should exist in the CodeMap", ndcCodeObj);

        // Tests the drug details
        String drugDetails = CodeMap.getDrugDetailsByNDC(ndcCode);
        assertNotNull("Drug details should not be null", drugDetails);
        assertEquals("Drug: NDC Label 1\nDescription: Description for NDC 1\nStatus: Valid", drugDetails);
    }


    @Test
    public void testGetRelatedNDCsForCVX_ExistingCVX() {
        String cvxCode = "001";  // Example of a valid CVX
        Code cvxCodeObj = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, cvxCode);
        
        assertNotNull("The CVX code should exist in the CodeMap", cvxCodeObj);

        // Tests the NDCs linked to the CVX
        List<String> relatedNDCs = codeMap.getRelatedValues(cvxCodeObj, CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE);
        
        assertNotNull("The related NDCs list should not be null", relatedNDCs);
        assertFalse("The related NDCs list should not be empty for this CVX", relatedNDCs.isEmpty());

        assertEquals(4, relatedNDCs.size());
        assertEquals("00001-0000-01", relatedNDCs.get(0));
        assertEquals("00001-0000-05", relatedNDCs.get(1));
        assertEquals("00001-0000-10", relatedNDCs.get(2));
    }


@Test
public void testGetMostFrequentNDCsByCVXList_MultipleCVXCodes() {

    // Call the method with the list of CVX codes
    List<String> sortedNDCs = codeMap.getMostFrequentNDCsByCVXList(Arrays.asList(cvxCodeObj1, cvxCodeObj2, cvxCodeObj3));

    // Check that the returned list is not null
    assertNotNull("The list of sorted NDCs should not be null", sortedNDCs);
    assertFalse("The list of sorted NDCs should not be empty", sortedNDCs.isEmpty());

    // Example validation of results - adjust based on test data
    assertEquals("00002-0000-01", sortedNDCs.get(0));  // Expected first position
}


@Test
public void testGetCombosByCVXList() {

    // Call the method with the list of CVX codes
    List<String> sortedNDCs = codeMap.getMostFrequentNDCsByCVXList(Arrays.asList(cvxCodeObj1, cvxCodeObj2, cvxCodeObj3));

    // Check that the returned list is not null
    assertNotNull("The list of sorted NDCs should not be null", sortedNDCs);
    assertFalse("The list of sorted NDCs should not be empty", sortedNDCs.isEmpty());

    // Example validation of results - adjust based on test data
    assertEquals("00002-0000-01", sortedNDCs.get(0));  // Expected first position
}

/*  Case 1 - Find two Combo objects with the CVX codes 004, 005, and 006
 * 
 */
@Test
public void testGetCombosByCVXListCase1() {

    // Call the method with the list of CVX codes
    List<Combo> comboList = codeMap.getCombosByCVXList(Arrays.asList(cvxCodeObj4, cvxCodeObj5, cvxCodeObj6));

    Combo combo1 = comboList.get(0);
    Combo combo2 = comboList.get(1);

    assertEquals(1, combo1.getNdcList().size());
    assertEquals(2, combo2.getNdcList().size());

    assertEquals("00003-0000-03", combo1.getNdcList().get(0).getNdcCode());
    // Test score combo 1
    assertEquals(100.00, combo1.getScore(), 0.00);

    // Combo 2
    assertEquals("00003-0000-02", combo2.getNdcList().get(0).getNdcCode());
    assertEquals("00003-0000-01", combo2.getNdcList().get(1).getNdcCode());
    // Test score combo 2
    assertEquals(50.00, combo2.getScore(), 0.00);
}



}
