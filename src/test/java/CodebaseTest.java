import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mitre.synthea.codebase.CodeMap;
import org.mitre.synthea.codebase.CodeMapBuilder;
import org.mitre.synthea.codebase.reference.*;
import org.mitre.synthea.codebase.generated.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CodebaseTest {


    private static CodeMap codeMap;

    @BeforeClass
    public static void setUp() {
        codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap();
    }

    @Test
    public void testGetDrugDetailsByNDC_ExistingNDC() {
        String ndcCode = "00005-0100-01";  // Example of an existing NDC
        Code ndcCodeObj = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE, ndcCode);
        
        assertNotNull("The NDC code should exist in the CodeMap", ndcCodeObj);

        // Tests the drug details
        String drugDetails = CodeMap.getDrugDetailsByNDC(ndcCode);
        assertNotNull("Drug details should not be null", drugDetails);
        assertEquals("Drug: Trumenba\nDescription: Pfizer - Meningococcal B, recombinant\nStatus: Valid", drugDetails);
    }


    @Test
    public void testGetDrugDetailsByNDC_NonExistingNDC() {
        String invalidNdcCode = "99999-9999-99";  // An NDC that does not exist in the CodeMap

        // Checks that the method returns the expected error message for a non-existent NDC
        String drugDetails = CodeMap.getDrugDetailsByNDC(invalidNdcCode);
        assertEquals("No drug found for NDC " + invalidNdcCode, drugDetails);
    }


    @Test
    public void testGetRelatedNDCForCVX_ExistingCVX() {
        String cvxCode = "162";  // Example of a valid CVX
        Code cvxCodeObj = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, cvxCode);
        
        assertNotNull("The CVX code should exist in the CodeMap", cvxCodeObj);

        // Tests the NDC linked to the CVX
        String relatedNDC = codeMap.getRelatedValueTest(cvxCodeObj, CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE);
        assertNotNull("The related NDC should exist for this CVX", relatedNDC);
        assertEquals("00005-0100-01", relatedNDC);
    }

}
