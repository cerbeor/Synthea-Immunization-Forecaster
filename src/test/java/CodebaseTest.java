import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mitre.synthea.codebase.CodeMap;
import org.mitre.synthea.codebase.CodeMapBuilder;
import org.mitre.synthea.codebase.generated.Code;
import org.mitre.synthea.codebase.reference.CodesetType;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CodebaseTest {


    private static CodeMap codeMap;

    @BeforeClass
    public static void setUp() {
        codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap("./src/test/resources/CompiledTest.xml");
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

        assertEquals(3, relatedNDCs.size());
        assertEquals("00001-0000-01", relatedNDCs.get(0));
        assertEquals("00001-0000-05", relatedNDCs.get(1));
        assertEquals("00001-0000-10", relatedNDCs.get(2));
    }



}
