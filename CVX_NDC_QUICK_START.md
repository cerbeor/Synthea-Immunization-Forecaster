# CVX to NDC Mapping - Quick Start

## TL;DR - Where is the code?

### Main Files:
1. **Mapping Logic**: `src/main/java/org/mitre/synthea/codebase/CodeMap.java` (790 lines)
2. **Mapping Creation**: `src/main/java/org/mitre/synthea/codebase/mapping/Mapping.java`
3. **Data Classes**: 
   - `src/main/java/org/mitre/synthea/codebase/mapping/NDC.java`
   - `src/main/java/org/mitre/synthea/codebase/mapping/CVX.java`
4. **Data Source**: `src/test/resources/Compiled.xml`
5. **Tests**: `src/test/java/CodebaseTest.java`

### Quick Usage:

```java
// Get the CodeMap
CodeMap codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap();

// Get NDC codes from a CVX code
Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "162");
List<String> ndcCodes = codeMap.getRelatedValues(cvxCode, CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE);

// Get drug details from NDC
String details = CodeMap.getDrugDetailsByNDC("00005-0100-01");
```

## File Locations Summary

```
src/main/java/org/mitre/synthea/codebase/
├── CodeMap.java                    # Main mapping class (MOST IMPORTANT)
├── CodeMapBuilder.java             # Loads data from XML
├── mapping/
│   ├── Mapping.java                # Creates NDC objects from CVX
│   ├── NDC.java                    # NDC representation
│   ├── CVX.java                    # CVX representation
│   ├── Combo.java                  # Combination of NDCs
│   ├── SpecificCase.java           # Special case handling
│   └── VaccineGroup.java           # Vaccine group mapping
└── reference/
    ├── CodesetType.java            # Enum of all code types
    ├── CvxConceptType.java         # CVX concepts
    └── CvxSpecialValues.java       # Special CVX values

src/test/resources/
└── Compiled.xml                    # XML database with all mappings

src/test/java/
├── CodebaseTest.java               # Main tests for CVX-NDC mapping
└── ComboTest.java                  # Tests for combinations
```

## Most Common Operations

### 1. CVX → NDC (one result)
```java
Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "162");
String ndc = codeMap.getRelatedValueTest(cvxCode, CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE);
```

### 2. CVX → All NDCs
```java
Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, "162");
List<String> ndcs = codeMap.getRelatedValues(cvxCode, CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE);
```

### 3. NDC → Details
```java
String details = CodeMap.getDrugDetailsByNDC("00005-0100-01");
```

### 4. Find NDC combinations for multiple CVX codes
```java
List<String> cvxCodes = Arrays.asList("001", "002", "003");
List<Combo> combos = codeMap.getCombosByCVXStringList(cvxCodes);
```

## Data Structure in Compiled.xml

```xml
<code>
    <value>00069-0207-01</value>                          <!-- NDC code -->
    <label>Abrysvo</label>                                <!-- Drug name -->
    <description>Pfizer - RSV vaccine</description>       <!-- Description -->
    <code-status>
        <status>Valid</status>                            <!-- Status -->
    </code-status>
    <reference>
        <link-to codeset="VACCINATION_CVX_CODE">305</link-to>         <!-- Links to CVX -->
        <link-to codeset="VACCINATION_MANUFACTURER_CODE">PFR</link-to> <!-- Manufacturer -->
    </reference>
    <use-date>
        <not-before>20230712</not-before>                 <!-- Valid from date -->
    </use-date>
</code>
```

## Running Tests

```bash
# Run all CVX/NDC mapping tests
./gradlew test --tests CodebaseTest

# Run specific test
./gradlew test --tests CodebaseTest.testGetRelatedNDCForCVX_ExistingCVX
```

## For More Details

See [CVX_NDC_MAPPING_GUIDE.md](CVX_NDC_MAPPING_GUIDE.md) for:
- Complete API documentation
- Architecture diagrams
- Extended examples
- All available methods
- Testing information
