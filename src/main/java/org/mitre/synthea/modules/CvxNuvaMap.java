package org.mitre.synthea.modules;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mitre.synthea.helpers.SimpleCSV;
import org.mitre.synthea.helpers.Utilities;

/**
 * Provides lookup capabilities for NUVA codes based on a CVX code.
 */
public final class CvxNuvaMap {

  private static final String RESOURCE_PATH = "immunizations/cvx_to_nuva_mapping.csv";
  private static final Map<String, Mapping> LOOKUP = loadMapping();

  private CvxNuvaMap() { }

  /**
   * Look up the NUVA mapping for the supplied CVX code.
   *
   * @param cvxCode the raw CVX value (with or without the leading "CVX" prefix)
   * @return a {@link Mapping} containing the NUVA code and label, or {@code null} when no mapping exists
   */
  public static Mapping findByCvx(String cvxCode) {
    if (cvxCode == null) {
      return null;
    }
    return LOOKUP.get(normalize(cvxCode));
  }

  private static Map<String, Mapping> loadMapping() {
    Map<String, Mapping> mappings = new HashMap<>();
    try {
      String csv = Utilities.readResource(RESOURCE_PATH, true, true);
      List<LinkedHashMap<String, String>> rows = SimpleCSV.parse(csv, ';');
      for (LinkedHashMap<String, String> row : rows) {
        String cvx = normalize(row.get("CVX"));
        String nuva = row.get("NUVA");
        if (cvx == null || cvx.isEmpty() || nuva == null || nuva.isEmpty()) {
          continue;
        }
        String label = row.get("Label");
        mappings.put(cvx, new Mapping(cvx, nuva.trim(), label));
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to load CVX to NUVA mapping", e);
    }
    return Collections.unmodifiableMap(mappings);
  }

  private static String normalize(String cvx) {
    if (cvx == null) {
      return null;
    }
    String normalized = cvx.trim();
    if (normalized.isEmpty()) {
      return normalized;
    }
    String upper = normalized.toUpperCase();
    if (upper.startsWith("CVX")) {
      normalized = normalized.substring(3);
      if (normalized.startsWith("-")) {
        normalized = normalized.substring(1);
      }
    }
    if (normalized.startsWith("-")) {
      normalized = normalized.substring(1);
    }
    return normalized;
  }

  /** Container for a single CVX to NUVA mapping entry. */
  public static final class Mapping {
    private final String cvxCode;
    private final String nuvaCode;
    private final String label;

    private Mapping(String cvxCode, String nuvaCode, String label) {
      this.cvxCode = cvxCode;
      this.nuvaCode = nuvaCode;
      this.label = label;
    }

    public String getCvxCode() {
      return cvxCode;
    }

    public String getNuvaCode() {
      return nuvaCode;
    }

    public String getLabel() {
      return label;
    }
  }
}
