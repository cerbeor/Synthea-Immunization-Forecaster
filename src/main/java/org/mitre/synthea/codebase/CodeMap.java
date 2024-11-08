package org.mitre.synthea.codebase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mitre.synthea.codebase.generated.Code;
import org.mitre.synthea.codebase.generated.Codebase;
import org.mitre.synthea.codebase.generated.Codeset;
import org.mitre.synthea.codebase.generated.LinkTo;
import org.mitre.synthea.codebase.generated.Reference;
import org.mitre.synthea.codebase.generated.UseDate;
import org.mitre.synthea.codebase.mapping.Combo;
import org.mitre.synthea.codebase.mapping.Mapping;
import org.mitre.synthea.codebase.mapping.NDC;
import org.mitre.synthea.codebase.reference.CodeStatusValue;
import org.mitre.synthea.codebase.reference.CodesetType;
import org.mitre.synthea.codebase.reference.Ops;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeMap {

  
  
  public Map<CodesetType, Map<String, Code>> getCodeBaseMap() {
      return codeBaseMap; // Retourne la carte des codes
  }

  private List<NDC> ndcList;

  private static final Logger logger = LoggerFactory.getLogger(CodeMap.class);

  private Map<CodesetType, Map<String, Code>> codeBaseMap;
  private Map<String, List<Code>> productMap = new HashMap<String, List<Code>>();
  private static final String MAP_DELIMITER = "::";
  private final DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyyMMdd");

  public List<Code> getProductsFor(String vaccineCvx, String vaccineMvx) {
    if (StringUtils.isBlank(vaccineCvx) || StringUtils.isBlank(vaccineMvx)) {
      return new ArrayList<Code>();
    }
    List<Code> productCodes = productMap.get(vaccineCvx + MAP_DELIMITER + vaccineMvx);
    if (productCodes == null) {
      return new ArrayList<Code>();
    }
    return productCodes;
  }

  // New 

    /**
   * Retrieves drug details based on a specified National Drug Code (NDC).
   * 
   * This method looks up the provided NDC within the default CodeMap, retrieving 
   * relevant details if the code is found, including the drug label, description, 
   * and status. If no drug is found for the given NDC, an error message is returned.
   *
   * @param ndcCode the NDC code to search for
   * @return a formatted string with drug details (label, description, status) if found; 
   *         otherwise, an error message indicating no drug was found
   */


  public static String getDrugDetailsByNDC(String ndcCode) {
    CodeMap codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap();
    Code ndcCodeObj = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE, ndcCode);
    
    if (ndcCodeObj != null) {
        String drugLabel = ndcCodeObj.getLabel();
        String description = ndcCodeObj.getDescription();
        String status = ndcCodeObj.getCodeStatus().getStatus();
        return String.format("Drug: %s\nDescription: %s\nStatus: %s", drugLabel, description, status);
    } else {
        return "No drug found for NDC " + ndcCode;
    }

  }

  
  /**
   * Retrieves the related value from a given code's reference links based on a specified codeset type.
   * 
   * This method checks the input code for an existing reference and searches its links for a match
   * with the desired codeset type. If a match is found, the associated value is returned; otherwise,
   * an empty string is returned.
   *
   * @param codeIn      the code to retrieve the related value from
   * @param desiredType the codeset type to match in the reference links
   * @return the related value if found; otherwise, an empty string
   */

  public String getRelatedValueTest(Code codeIn, CodesetType desiredType) { 
    String relatedValue = "";
    if (codeIn == null) {
        logger.warn("The input code is null.");
        return relatedValue;
    }
    if (desiredType == null) {
        logger.warn("The desired type is null.");
        return relatedValue;
    }

    Reference r = codeIn.getReference();
    if (r == null) {
        logger.warn("No reference found for the code: {}", codeIn.getValue());
        return relatedValue;
    }

    List<LinkTo> linkList = r.getLinkTo();
    if (linkList.isEmpty()) {
        logger.warn("No links found in the reference for the code: {}", codeIn.getValue());
    } else {
        for (LinkTo link : linkList) {
            CodesetType linkCodesetType = CodesetType.getByTypeCode(link.getCodeset());
            logger.info("Checking link {} for the code: {} with codeset type: {}", link.getValue(), codeIn.getValue(), linkCodesetType);
            if (desiredType.equals(linkCodesetType)) {
                relatedValue = link.getValue();
                logger.info("Value found: {}", relatedValue);
                break;
            }
        }
    }

    return relatedValue;
}




  public List<String> getRelatedValues(Code codeIn, CodesetType desiredType) {
      List<String> relatedValues = new ArrayList<>(); // Using a list to store multiple values
      if (codeIn == null) {
          return relatedValues; // Returns an empty list if codeIn is null
      }
      if (desiredType == null) {
          return relatedValues; // Returns an empty list if desiredType is null
      }
      Reference r = codeIn.getReference();
      if (r == null) {
          return relatedValues; // Returns an empty list if the reference is null
      }

      List<LinkTo> linkList = r.getLinkTo();
      for (LinkTo link : linkList) {
          if (desiredType.equals(CodesetType.getByTypeCode(link.getCodeset()))) {
              relatedValues.add(link.getValue()); // Adds each related value to the list
          }
      }

      return relatedValues; // Returns the list of related values
  }



  // 


    public List<String> getMostFrequentNDCsByCVXList(List<Code> cvxCodes) {
      Map<String, Integer> ndcFrequency = new HashMap<>();

      for (Code cvxCode : cvxCodes) {
          List<String> relatedNDCs = getRelatedValues(cvxCode, CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE);

          // Compter chaque NDC dans le Map de fréquence
          for (String ndc : relatedNDCs) {
              ndcFrequency.put(ndc, ndcFrequency.getOrDefault(ndc, 0) + 1);
          }
      }

      // Filtrer les NDC présents dans toutes les listes si nécessaire
      int totalCvxCount = cvxCodes.size();
      List<String> ndcInAllLists = ndcFrequency.entrySet().stream()
          .filter(entry -> entry.getValue() == totalCvxCount)
          .map(Map.Entry::getKey)
          .collect(Collectors.toList());

      // Sinon, trier les NDC par fréquence décroissante
      List<String> sortedNDCs = ndcFrequency.entrySet().stream()
          .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
          .map(Map.Entry::getKey)
          .collect(Collectors.toList());

      return ndcInAllLists.isEmpty() ? sortedNDCs : ndcInAllLists;
  }

  //  Combo

    /**
     * This method uses the Mapping class to create vaccine combos
     * from a list of CVX codes.
     *
     * @param cvxCodes List of CVX codes.
     * @return List of combos generated from the provided CVX codes.
     */
    public List<Combo> getCombosByCVXList(List<Code> cvxCodes) {
        // Instantiate the Mapping class with the current CodeMap instance
        Mapping mapping = new Mapping(this);
        
        // Step 1: Create NDCs from the provided CVX codes
        List<NDC> ndcList = mapping.createNDCsFromCVX(cvxCodes);
        
        // Debugging output to verify NDC creation
        System.out.println("NDC List Created:");
        for (NDC ndc : ndcList) {
            System.out.println(ndc);
        }
        System.out.println("End of NDC List\n");

        // Step 2: Generate combos from the list of NDCs
        List<Combo> comboList = mapping.createCombosFromNDCs(ndcList, cvxCodes);
        
        // Step 3: Debugging output to verify Combo creation
        System.out.println("Generated Combos:");
        for (Combo combo : comboList) {
            System.out.println(combo);
        }
        System.out.println("End of Combos\n");

        // Step 4: Return the list of generated combos
        return comboList;
    }




// 



    

  public String getRelatedValue(Code codeIn, CodesetType desiredType) {
    String relatedValue = "";
    if (codeIn == null) {
      return relatedValue;
    }
    if (desiredType == null) {
      return relatedValue;
    }
    Reference r = codeIn.getReference();
    if (r == null) {
      return relatedValue;
    }

    List<LinkTo> linkList = r.getLinkTo();
    for (LinkTo link : linkList) {
      if (desiredType.equals(CodesetType.getByTypeCode(link.getCodeset()))) {
        relatedValue = link.getValue();
        break;
      }
    }

    return relatedValue;
  }



  public Code getProductFor(String vaccineCvx, String vaccineMvx, String adminDate) {
    List<Code> productCodes = getProductsFor(vaccineCvx, vaccineMvx);

    if (productCodes.size() == 1) {
      return productCodes.get(0);
    } else if (productCodes.size() == 0) {
      return null;
    }

    //at this point we know we have more than one product in our list.
    // time to evaluate the dates to see which one is active.
    if (logger.isTraceEnabled()) {
      logger.trace("There is more than one product for cvx[" + vaccineCvx + "] + mvx[" + vaccineMvx
          + "] checking dates");
    }

    //We need to pick the most likely one.
    if (adminDate != null) {
      if (logger.isTraceEnabled()) {
        logger.trace("Searching for product that is valid for admin date[" + adminDate + "]");
      }

      DateTime adminDT = parseDateTime(adminDate);

      //At this point, we know there's two or more products.
      for (Code productCode : productCodes) {
        //Now we need to chose the right one based on the dates.
        UseDate dates = productCode.getUseDate();

        String beginning = dates.getNotBefore();

        //Parse the dates...
        DateTime bDT = parseDateTime(beginning);
        if (logger.isTraceEnabled()) {
          logger.trace("Must be after getNotBefore[" + beginning + "] ");
        }
        boolean isAfterBeginning = !isBeforeThis(adminDT, bDT);
        if (logger.isTraceEnabled()) {
          logger.trace("isAfterBeginning ? " + isAfterBeginning);
        }

        //We only care about the beginning date.
        //This is very intentional, and it allows us to be flexible and return products where
        //the admin date isn't exactly within the range, but its the closest one.
        //This properly deals with gaps, and with overlapping ranges.
        if (isAfterBeginning) {
          return productCode;
        }
      }

      //If we get here, there are multiple and the admin date is before ALL of the beginning dates
      //so just return the last one in the list.
      return productCodes.get(productCodes.size() - 1);
    }

    return null;
  }

  protected DateTime parseDateTime(String inDate) {
    if (inDate != null) {
      try {
        return dtf.parseDateTime(inDate);
      } catch (IllegalArgumentException iae) {
        return null;
      }
    }
    return null;
  }

  protected boolean isAfterThis(DateTime isThis, DateTime afterThis) {
    if (isThis != null) {
      boolean isAfterThis = (afterThis != null && isThis.isAfter(afterThis));
      return isAfterThis;
    }
    return false;
  }

  protected boolean isBeforeThis(DateTime isThis, DateTime beforeThis) {
    if (isThis != null) {
      boolean isBefore = (beforeThis != null && isThis.isBefore(beforeThis));
      return isBefore;
    }
    return false;
  }


  private Codebase base;

  public CodeMap(Codebase mapthis) {
    remap(mapthis);
  }

  public CodeMap() {
    //default constructor.
  }

  public void remap(Codebase mapthis) {
    this.base = mapthis;

    if (logger.isTraceEnabled()) {
      logger.trace("CodeBase Mapping!");
    }

    this.codeBaseMap = new HashMap<CodesetType, Map<String, Code>>();
    for (Codeset s : mapthis.getCodeset()) {
      //Get the type for this set:
      CodesetType typeCode = CodesetType.getByTypeCode(s.getType());

      //Map the codeset:
      if (logger.isInfoEnabled()) {
        logger.info("Mapping codeset: " + s.getType());
      }

      Map<String, Code> codeMap = new HashMap<String, Code>();
      for (Code c : s.getCode()) {
        codeMap.put(c.getValue(), c);
        if (logger.isTraceEnabled()) {
          logger.trace(
              "     " + s.getType() + " value[" + c.getValue() + "]  for code[" + c.getLabel()
                  + "]");
        }
      }

      this.codeBaseMap.put(typeCode, codeMap);

      //Special Handling for NDC Codes.  Add it to an additional CodesetType.
      if (typeCode == CodesetType.VACCINATION_NDC_CODE_UNIT_OF_SALE
          || typeCode == CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE) {
        Map<String, Code> ndcCodeMap = this.codeBaseMap.get(CodesetType.VACCINATION_NDC_CODE);
        //The first NDC set will need to build a map.
        if (ndcCodeMap == null) {
          ndcCodeMap = new HashMap<>();
          this.codeBaseMap.put(CodesetType.VACCINATION_NDC_CODE, ndcCodeMap);
        }
        ndcCodeMap.putAll(codeMap);
      }

      //Special Handling for Vaccine Product
      if (typeCode == CodesetType.VACCINE_PRODUCT) {
        for (Code productCode : s.getCode()) {
          Reference ref = productCode.getReference();
          if (ref != null) {
            List<LinkTo> links = ref.getLinkTo();
            String productCvx = null;
            String productMvx = null;
            if (links != null) {
              for (LinkTo link : links) {
                String codeset = link.getCodeset();
                switch (CodesetType.getByTypeCode(codeset)) {
                  case VACCINATION_CVX_CODE:
                    productCvx = link.getValue();
                    break;
                  case VACCINATION_MANUFACTURER_CODE:
                    productMvx = link.getValue();
                    break;
                  default:
                    break;
                }
              }
            }
            if (productCvx != null && productMvx != null) {
              List<Code> products = productMap.get(productCvx + MAP_DELIMITER + productMvx);
              if (products == null) {
                products = new ArrayList<Code>();
                productMap.put(productCvx + MAP_DELIMITER + productMvx, products);
              }

              products.add(productCode);

              if (products.size() > 1) {
                if (logger.isTraceEnabled()) {
                  logger.trace("SORTING PRODUCT: " + productCode.getValue());
                }
                Collections.sort(products, new CustomCodeDateComparator());
              }
            }
          }
        }
      }
    }
  }

  /**
   * If the products both have a date, they will sort by that date.  if either one doesn't have a date, they
   * will be considered equal, and sort in the order they come in.
   * @author Josh
   *
   */
  public class CustomCodeDateComparator implements Comparator<Code> {

    public int compare(Code o1, Code o2) {
      UseDate dates = o1.getUseDate();
      UseDate dates2 = o2.getUseDate();

      if (dates != null && dates2 != null) {
        String date1 = dates.getNotBefore();
        if (date1 == null) {
          date1 = dates.getNotAfter();
        }

        String date2 = dates2.getNotBefore();

        if (date2 == null) {
          date2 = dates2.getNotAfter();
        }

        if (date2 != null && date1 != null) {
          return date2.compareTo(date1);//Want a descending sort.
        }
      }
      return 0;
    }
  }

  public Code getCodeForCodeset(CodesetType c, String value) {
    return this.getCodeForCodeset(c, value, Ops.Mapping.MAP);
  }

  Code checkVariants(CodesetType c, String value) {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    //Sometimes bar code scanners introduce a 12th character into the code...
    //let's try to fix it if that caused problems.
    if (
        (value.length() == 12 || value.length() == 14) //dashes or no dashes
        && (c == CodesetType.VACCINATION_NDC_CODE
         || c == CodesetType.VACCINATION_NDC_CODE_UNIT_OF_SALE
         || c == CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE )) {
      return codeBaseMap.get(c).get(value.substring(1));
    }

    return null;
  }

  public Code getCodeForCodeset(CodesetType c, String value, Ops.Mapping mappingOption) {
    Code code = null;

    if (StringUtils.isBlank(value)) {
      return code;
    }

//		1. Get the codeset
    Map<String, Code> codeSetMap = codeBaseMap.get(c);

    if (codeSetMap != null) {

//			2. get the code
      code = codeSetMap.get(value);

//      2.1 if the code isn't found, check for some known variations on expressing the codes.
      if (code == null) {
        code = checkVariants(c, value);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("found code: " + code);
        if (code != null) {
          logger.debug(code.getLabel() != null && code.getCodeStatus() != null ? " status: " + code.getCodeStatus().getStatus() : " status: null");
        }
      }

//			3. Check if it's mapped to something else by deprecation.
      if (code != null && mappingOption == Ops.Mapping.MAP) {
        CodeStatusValue status = CodeStatusValue.getBy(code.getCodeStatus());
        if (status == CodeStatusValue.DEPRECATED
            && code.getCodeStatus().getDeprecated() != null
            ) {
          String newValue = code.getCodeStatus().getDeprecated().getNewCodeValue();
          if (logger.isDebugEnabled()) {
            logger.debug("Mapping to: " + newValue);
          }
          code = codeSetMap.get(newValue);
        }
      }
    }

    return code;
  }

  public Code getFirstRelatedCodeForCodeIn(CodesetType codeTypeIn, String codeIn,
      CodesetType codeTypeDesired) {
    List<Code> relatedCodes = this.getRelatedCodesForCodeIn(codeTypeIn, codeIn, codeTypeDesired);
    if (relatedCodes != null && relatedCodes.size() > 0) {
      return relatedCodes.get(0);
    }
    return null;
  }

  /**
   * This is a shorcut method.  It gives access to the related codes
   * without first getting a Code object.
   * @param ct
   * @param value
   * @return
   */
  public Map<CodesetType, List<Code>> getRelatedCodes(CodesetType ct, String value) {
    Code code = this.getCodeForCodeset(ct, value);
    Map<CodesetType, List<Code>> links = getRelatedCodes(code);
    return links;
  }

  public List<Code> getRelatedCodesForCodeIn(CodesetType codeTypeIn, String codeIn,
      CodesetType codeTypeDesired) {
    Map<CodesetType, List<Code>> relatedCodes = this.getRelatedCodes(codeTypeIn, codeIn);
    return relatedCodes.get(codeTypeDesired);
  }

  /**
   * This returns the first related code found, if present
   *
   * @param code The code where the reference should be taken from
   * @param relatedCodesetType The reference codeset that needs to be looked up
   * @return
   */
  public String getRelatedCodeValue(Code code, CodesetType relatedCodesetType) {
    if (code != null && code.getReference() != null) {
      List<LinkTo> links = code.getReference().getLinkTo();
      for (LinkTo link : links) {
        if (link != null) {
          String codesetType = link.getCodeset();
          CodesetType type = CodesetType.getByTypeCode(codesetType);
          if (type == relatedCodesetType) {
            return link.getValue();
          }
        }
      }
    }
    return null;
  }

  /**
   * This returns the first related code found, if present
   *
   * @param code The code where the reference should be taken from
   * @param relatedCodesetType The reference codeset that needs to be looked up
   * @return
   */
  public Code getRelatedCode(Code code, CodesetType relatedCodesetType) {
    String relatedValue = getRelatedCodeValue(code, relatedCodesetType);
    if (StringUtils.isNotBlank(relatedValue)) {
      return this.getCodeForCodeset(relatedCodesetType, relatedValue);
    }
    return null;
  }

  /**
   * Gets the list of codes defined as related to the code sent in.
   * @param c
   * @return Map of Types, and the associated codes.
   */
  public Map<CodesetType, List<Code>> getRelatedCodes(Code c) {
    Map<CodesetType, List<Code>> relatedCodes = new HashMap<CodesetType, List<Code>>();
    if (c != null && c.getReference() != null) {
      List<LinkTo> links = c.getReference().getLinkTo();

      for (LinkTo link : links) {
        if (link != null) {
          String codesetType = link.getCodeset();
          CodesetType type = CodesetType.getByTypeCode(codesetType);
          List<Code> codeList = relatedCodes.get(type);
          if (codeList == null) {
            codeList = new ArrayList<Code>();
            relatedCodes.put(type, codeList);
          }
          Code relatedCode = this.getCodeForCodeset(type, link.getValue());
          codeList.add(relatedCode);
        }
      }
    }
    return relatedCodes;
  }

  public Collection<Code> getCodesForTable(CodesetType c) {
    Map<String, Code> codeSetMap = codeBaseMap.get(c);
    if (codeSetMap == null)
    {
      return null;
    }
    return codeSetMap.values();
  }

  public List<Codeset> getCodesets() {
    return base.getCodeset();
  }

  /**
   * A helper method to compare two codes.  The Code object is a generated class, so anything put there
   * would get erased the next time its generated.
   * @param code1
   * @param code2
   * @return
   */
  public boolean codeEquals(Code code1, Code code2) {
    if (code1.getConceptType() == null) {
      if (code2.getConceptType() != null) {
        return false;
      }
    } else if (!code1.getConceptType().equals(code2.getConceptType())) {
      return false;
    }

    if (code1.getValue() == null) {
      if (code2.getValue() != null) {
        return false;
      }
    } else if (!code1.getValue().equals(code2.getValue())) {
      return false;
    }

    return true;
  }
}
