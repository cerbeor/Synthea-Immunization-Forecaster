# CVX to NDC Mapping Code Location Guide

This document explains where the code related to the mapping between CVX (Clinical Vaccine Code) and NDC (National Drug Code) codes is located in the Synthea-Immunization-Forecaster repository.

## Table of Contents
1. [Overview](#overview)
2. [Core Mapping Classes](#core-mapping-classes)
3. [Data Source](#data-source)
4. [Key Methods and Usage](#key-methods-and-usage)
5. [Examples](#examples)
6. [Testing](#testing)

## Overview

The CVX to NDC mapping system in Synthea is a comprehensive code management system designed to store and organize various healthcare-related codes, particularly for vaccines and medications. The system enables bidirectional mapping between different code types (CVX, NDC, vaccine groups, manufacturers, etc.) and is used throughout the immunization forecasting functionality.

## Core Mapping Classes

### 1. Main Classes

#### **`src/main/java/org/mitre/synthea/codebase/CodeMap.java`**
The central class for managing all code mappings. This class:
- Stores mappings between different code sets (CVX, NDC, CPT, vaccine groups, etc.)
- Provides methods to retrieve related codes across different code systems
- Manages relationships using a `Map<CodesetType, Map<String, Code>>` structure

**Key methods:**
```java
// Get a Code object for a specific code set and value
Code getCodeForCodeset(CodesetType type, String value)

// Get related NDC codes from a CVX code
List<String> getRelatedValues(Code codeIn, CodesetType desiredType)

// Get a single related value (useful for one-to-one mappings)
String getRelatedValueTest(Code codeIn, CodesetType desiredType)

// Get drug details by NDC code
static String getDrugDetailsByNDC(String ndcCode)

// Get most frequent NDCs for a list of CVX codes
List<String> getMostFrequentNDCsByCVXList(List<Code> cvxCodes)

// Get combinations of NDCs that cover a list of CVX codes
List<Combo> getCombosByCVXStringList(List<String> cvxCodes)
```

**Line count:** ~790 lines

---

#### **`src/main/java/org/mitre/synthea/codebase/mapping/Mapping.java`**
Facilitates the creation of NDC objects and their combinations based on related CVX codes.

**Key methods:**
```java
// Create NDC objects from a list of CVX codes
List<NDC> createNDCsFromCVX(List<Code> cvxCodes)

// Create NDC objects from CVX code strings
List<NDC> createNDCsFromCVXString(List<String> cvxCodes)

// Create combinations of NDCs to match target CVX codes
List<Combo> createCombosFromNDCs(List<NDC> ndcList, List<Code> targetCvxList)
```

**Algorithm:** Uses backtracking to find optimal combinations of NDC codes that cover all target CVX codes, with optimization for fewer NDCs while ensuring complete coverage.

---

#### **`src/main/java/org/mitre/synthea/codebase/mapping/NDC.java`**
Represents a National Drug Code (NDC) and its associated information.

**Key fields:**
- `ndcCode`: The NDC code string
- `cvxCodes`: List of related CVX codes
- `vaccineGroups`: List of vaccine group codes
- `previousCodes`: Historical codes for reverse lookups

**Key methods:**
```java
List<Code> getCvxCodes()
void addPreviousCode(Code code, CodeMap codeMap)
String reverseCode(Code cvxCode, CodeMap codeMap)
```

---

#### **`src/main/java/org/mitre/synthea/codebase/mapping/CVX.java`**
Represents a CVX (Clinical Vaccine Code) and its associated vaccine groups.

**Key fields:**
- `code`: The CVX code string
- `vaccineGroups`: List of vaccine groups linked to this CVX

---

#### **`src/main/java/org/mitre/synthea/codebase/mapping/Combo.java`**
Represents a combination of NDC codes that together cover a set of target CVX codes.

---

### 2. Supporting Classes

#### **`src/main/java/org/mitre/synthea/codebase/CodeMapBuilder.java`**
Singleton class responsible for building and loading CodeMap objects from XML files.

**Key methods:**
```java
// Get the default compiled CodeMap
CodeMap getDefaultCodeMap()

// Get CodeMap from a specific file path
CodeMap getCompiledCodeMap(String path)

// Build CodeMap from an input stream
CodeMap getCodeMap(InputStream inputStream)
```

**Default path:** Loads from `./src/test/resources/Compiled.xml` or `Compiled.xml` from classpath.

---

#### **`src/main/java/org/mitre/synthea/codebase/mapping/SpecificCase.java`**
Handles special cases and reverse mappings for specific CVX code scenarios.

---

#### **`src/main/java/org/mitre/synthea/codebase/mapping/VaccineGroup.java`**
Represents vaccine groups and their relationships.

---

#### **`src/main/java/org/mitre/synthea/codebase/reference/CodesetType.java`**
Enum defining all codeset types in the system, including:
- `VACCINATION_CVX_CODE`
- `VACCINATION_NDC_CODE_UNIT_OF_USE`
- `VACCINATION_NDC_CODE_UNIT_OF_SALE`
- `VACCINATION_MANUFACTURER_CODE`
- `VACCINE_GROUP`
- And many others

---

## Data Source

### **`src/test/resources/Compiled.xml`**
This is the main XML file containing all code mappings. It's a comprehensive database that includes:

**Structure:**
```xml
<codebase>
    <codeset>
        <label>Vaccination NDC Code Unit Of Use</label>
        <type>VACCINATION_NDC_CODE_UNIT_OF_USE</type>
        <code>
            <value>00069-0207-01</value>
            <label>Abrysvo</label>
            <description>Pfizer Laboratories Div Pfizer Inc - respiratory syncytial virus (RSV)...</description>
            <code-status>
                <status>Valid</status>
            </code-status>
            <reference>
                <link-to codeset="VACCINATION_CVX_CODE">305</link-to>
                <link-to codeset="VACCINATION_MANUFACTURER_CODE">PFR</link-to>
                <link-to codeset="VACCINATION_NDC_CODE_UNIT_OF_SALE">00069-0344-01</link-to>
            </reference>
            <use-date>
                <not-before>20230712</not-before>
            </use-date>
        </code>
        ...
    </codeset>
</codebase>
```

**Key features:**
- Each NDC code has `<link-to>` elements that reference related CVX codes
- Includes validity dates (`use-date` with `not-before`, `not-after`, etc.)
- Contains manufacturer information
- Links to both unit-of-use and unit-of-sale NDC codes

---

## Key Methods and Usage

### 1. Get NDC codes from a CVX code

```java
// Initialize CodeMap
CodeMap codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap();

// Get CVX Code object
Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "162");

// Get all related NDC codes
List<String> ndcCodes = codeMap.getRelatedValues(cvxCode, CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE);
// Result: ["00005-0100-01"]

// Or get a single NDC (first match)
String singleNdc = codeMap.getRelatedValueTest(cvxCode, CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE);
// Result: "00005-0100-01"
```

### 2. Get drug details from an NDC code

```java
String ndcCode = "00005-0100-01";
String details = CodeMap.getDrugDetailsByNDC(ndcCode);
// Result: "Drug: Trumenba\nDescription: Pfizer - Meningococcal B, recombinant\nStatus: Valid"
```

### 3. Create NDC objects from CVX codes

```java
CodeMap codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap();
Mapping mapping = new Mapping(codeMap);

// From CVX code strings
List<String> cvxCodes = Arrays.asList("162", "163", "164");
List<NDC> ndcs = mapping.createNDCsFromCVXString(cvxCodes);

// From CVX Code objects
List<Code> cvxCodeObjects = Arrays.asList(
    codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "162"),
    codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "163")
);
List<NDC> ndcs2 = mapping.createNDCsFromCVX(cvxCodeObjects);
```

### 4. Find NDC combinations that cover multiple CVX codes

```java
CodeMap codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap();
Mapping mapping = new Mapping(codeMap);

// Get target CVX codes
List<Code> targetCvx = Arrays.asList(
    codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "162"),
    codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "163")
);

// Create NDCs from these CVX codes
List<NDC> ndcs = mapping.createNDCsFromCVX(targetCvx);

// Find combinations that cover all CVX codes
List<Combo> combos = mapping.createCombosFromNDCs(ndcs, targetCvx);
// Returns optimal combinations of NDCs, sorted by number of NDCs (fewer is better)
```

### 5. Get most frequent NDCs for a list of CVX codes

```java
CodeMap codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap();

List<Code> cvxCodes = Arrays.asList(
    codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "162"),
    codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "163")
);

List<String> frequentNdcs = codeMap.getMostFrequentNDCsByCVXList(cvxCodes);
// Returns NDCs sorted by frequency across the CVX codes
```

---

## Examples

### Example 1: Complete CVX to NDC Lookup

```java
import org.mitre.synthea.codebase.CodeMap;
import org.mitre.synthea.codebase.CodeMapBuilder;
import org.mitre.synthea.codebase.generated.Code;
import org.mitre.synthea.codebase.reference.CodesetType;
import java.util.List;

public class CvxNdcExample {
    public static void main(String[] args) {
        // Load the CodeMap
        CodeMap codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap();
        
        // CVX code for Meningococcal B vaccine
        String cvxCode = "162";
        
        // Get the Code object
        Code cvxCodeObj = codeMap.getCodeForCodeset(
            CodesetType.VACCINATION_CVX_CODE, 
            cvxCode
        );
        
        if (cvxCodeObj != null) {
            System.out.println("CVX Code: " + cvxCode);
            System.out.println("Label: " + cvxCodeObj.getLabel());
            System.out.println("Description: " + cvxCodeObj.getDescription());
            
            // Get all related NDC codes
            List<String> ndcCodes = codeMap.getRelatedValues(
                cvxCodeObj, 
                CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE
            );
            
            System.out.println("\nRelated NDC codes:");
            for (String ndc : ndcCodes) {
                System.out.println("  - " + ndc);
                String details = CodeMap.getDrugDetailsByNDC(ndc);
                System.out.println("    " + details.replace("\n", "\n    "));
            }
        }
    }
}
```

### Example 2: Find Vaccine Combinations

```java
import org.mitre.synthea.codebase.CodeMap;
import org.mitre.synthea.codebase.CodeMapBuilder;
import org.mitre.synthea.codebase.mapping.Mapping;
import org.mitre.synthea.codebase.mapping.Combo;
import org.mitre.synthea.codebase.mapping.NDC;
import java.util.List;

public class VaccineCombinationExample {
    public static void main(String[] args) {
        CodeMap codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap();
        Mapping mapping = new Mapping(codeMap);
        
        // List of CVX codes needed
        List<String> requiredCvx = Arrays.asList("001", "002", "003");
        
        // Get combinations of NDCs that cover all CVX codes
        List<Combo> combos = codeMap.getCombosByCVXStringList(requiredCvx);
        
        System.out.println("Found " + combos.size() + " combinations:");
        for (int i = 0; i < Math.min(5, combos.size()); i++) {
            Combo combo = combos.get(i);
            System.out.println("\nCombination " + (i + 1) + ":");
            for (NDC ndc : combo.getNdcList()) {
                System.out.println("  NDC: " + ndc.getNdcCode());
                System.out.println("  Covers CVX codes: " + 
                    ndc.getCvxCodes().stream()
                        .map(c -> c.getValue())
                        .collect(Collectors.joining(", ")));
            }
        }
    }
}
```

---

## Testing

### Test Files

#### **`src/test/java/CodebaseTest.java`**
Contains unit tests for CVX to NDC mapping functionality:

**Key test methods:**
- `testGetDrugDetailsByNDC_ExistingNDC()`: Tests retrieving drug details by NDC
- `testGetDrugDetailsByNDC_NonExistingNDC()`: Tests error handling for invalid NDCs
- `testGetRelatedNDCForCVX_ExistingCVX()`: Tests getting a single NDC from CVX
- `testGetRelatedNDCsForCVX_ExistingCVX()`: Tests getting all NDCs for a CVX
- `testGetVaccineGroupLabelsFromCvx()`: Tests vaccine group retrieval

**Example test:**
```java
@Test
public void testGetRelatedNDCForCVX_ExistingCVX() {
    String cvxCode = "162";
    Code cvxCodeObj = codeMap.getCodeForCodeset(
        CodesetType.VACCINATION_CVX_CODE, 
        cvxCode
    );
    
    assertNotNull("The CVX code should exist", cvxCodeObj);
    
    String relatedNDC = codeMap.getRelatedValueTest(
        cvxCodeObj, 
        CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE
    );
    
    assertNotNull("The related NDC should exist", relatedNDC);
    assertEquals("00005-0100-01", relatedNDC);
}
```

#### **`src/test/java/ComboTest.java`**
Contains tests for combination generation:
- Tests creating combinations of vaccines
- Tests CVX to NDC mapping in combination scenarios

---

## Architecture Summary

```
┌─────────────────────────────────────────────┐
│         Compiled.xml                        │
│  (CVX-NDC mapping data source)              │
└──────────────────┬──────────────────────────┘
                   │ loaded by
                   ▼
┌─────────────────────────────────────────────┐
│      CodeMapBuilder                         │
│  (Singleton - loads XML into memory)        │
└──────────────────┬──────────────────────────┘
                   │ creates
                   ▼
┌─────────────────────────────────────────────┐
│         CodeMap                             │
│  - Stores all code mappings                 │
│  - Provides lookup methods                  │
│  - getCodeForCodeset()                      │
│  - getRelatedValues()                       │
└──────────────────┬──────────────────────────┘
                   │ used by
         ┌─────────┴─────────┬─────────────┐
         ▼                   ▼             ▼
    ┌─────────┐       ┌──────────┐   ┌────────┐
    │ Mapping │       │   NDC    │   │  CVX   │
    │  Class  │       │  Class   │   │ Class  │
    └─────────┘       └──────────┘   └────────┘
         │
         ▼
    ┌─────────┐
    │  Combo  │
    │  Class  │
    └─────────┘
```

---

## Related Files

### Immunization Modules
- **`src/main/java/org/mitre/synthea/modules/Immunizations.java`**: Main immunization module
- **`src/main/java/org/mitre/synthea/modules/covid/C19ImmunizationModule.java`**: COVID-19 specific immunizations

### Reference Classes
- **`src/main/java/org/mitre/synthea/codebase/reference/CodesetType.java`**: Enum of all codeset types
- **`src/main/java/org/mitre/synthea/codebase/reference/CvxConceptType.java`**: CVX concept type definitions
- **`src/main/java/org/mitre/synthea/codebase/reference/CvxSpecialValues.java`**: Special CVX values handling

### Utility Classes
- **`src/main/java/org/mitre/synthea/codebase/CodeMapUtil.java`**: Utility methods for CodeMap operations
- **`src/main/java/org/mitre/synthea/codebase/RelatedCode.java`**: Helper for related code lookups

---

## Quick Reference

| Task | Method | Class |
|------|--------|-------|
| Get NDCs from CVX | `getRelatedValues(cvxCode, VACCINATION_NDC_CODE_UNIT_OF_USE)` | CodeMap |
| Get CVX from string | `getCodeForCodeset(VACCINATION_CVX_CODE, "162")` | CodeMap |
| Get drug details | `getDrugDetailsByNDC(ndcCode)` | CodeMap (static) |
| Create NDC objects | `createNDCsFromCVXString(cvxCodes)` | Mapping |
| Find combinations | `createCombosFromNDCs(ndcList, targetCvxList)` | Mapping |
| Most frequent NDCs | `getMostFrequentNDCsByCVXList(cvxCodes)` | CodeMap |
| Load CodeMap | `getDefaultCodeMap()` | CodeMapBuilder |

---

## Conclusion

The CVX to NDC mapping system in Synthea is well-structured and provides comprehensive functionality for:
1. Bidirectional code lookups
2. Combination generation for multi-vaccine scenarios
3. Historical code tracking
4. Manufacturer and vaccine group associations
5. Temporal validity tracking

All mapping data is centralized in `Compiled.xml` and accessed through the `CodeMap` class, making it easy to maintain and extend.
