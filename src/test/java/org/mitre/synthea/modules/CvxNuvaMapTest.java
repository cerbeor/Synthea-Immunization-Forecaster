package org.mitre.synthea.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CvxNuvaMapTest {

  @Test
  public void findsMappingByCvxCode() {
    CvxNuvaMap.Mapping mapping = CvxNuvaMap.findByCvx("188");
    assertNotNull(mapping);
    assertEquals("VAC0906", mapping.getNuvaCode());
    assertEquals("188", mapping.getCvxCode());
  }

  @Test
  public void gracefullyHandlesUnknownCode() {
    assertNull(CvxNuvaMap.findByCvx("UNKNOWN"));
  }

  @Test
  public void acceptsPrefixedCvxValue() {
    CvxNuvaMap.Mapping mapping = CvxNuvaMap.findByCvx("CVX-188");
    assertNotNull(mapping);
    assertEquals("VAC0906", mapping.getNuvaCode());
  }
}
