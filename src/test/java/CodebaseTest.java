import static org.junit.Assert.assertEquals;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CodebaseTest {


  // /**
  //  * Configure settings across these tests.
  //  * @throws Exception on test configuration loading errors.
  //  */
  // @BeforeClass
  // public static void testSetup() throws Exception {

  // }

  @Test
  public void firstTest() throws Exception {
    assertEquals(1, 1);
  }

}
