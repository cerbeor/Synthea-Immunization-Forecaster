package org.mitre.synthea.modules;

import com.google.gson.Gson;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.DateType;
import org.immregistries.vfa.connect.ConnectFactory;
import org.immregistries.vfa.connect.ConnectorInterface;
import org.immregistries.vfa.connect.model.*;
import org.mitre.synthea.codebase.CodeMap;
import org.mitre.synthea.codebase.CodeMapBuilder;
import org.mitre.synthea.codebase.mapping.Combo;
import org.mitre.synthea.codebase.mapping.NDC;
import org.mitre.synthea.codebase.reference.CodesetType;
import org.mitre.synthea.helpers.Attributes;
import org.mitre.synthea.helpers.Attributes.Inventory;
import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.Code;


/**
 * For the news forecast functionality
 */
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

/**
 * CUSTOMIZED for industry project
 * added performEncounterWithForecaster method
 * TODO map forecasting objects
 * TODO test
 * TODO maybe move to another class and refactor all calls to immunizationSchedule object
 *
 */
public class Immunizations {
  public static final String IMMUNIZATIONS = "immunizations";

  /** Name of the two immunization servers (florence and test)*/
  public static final String FLORENCE = "https://florence.immregistries.org/step/fhir";
  public static final String TEST = "http://localhost:9999/fhir";
  /** Server that will be used for the immunization forecaster */
  private static String immunizationServer = FLORENCE; // default value

  /** Probability of a normal person not taking a vaccine */
  private static double noVaccineProbability = 10; // default value
  /** Probability of an antivax person not taking a vaccine */
  private static double noVaccineProbabilityAntivax = 90; // default value
  /** Probability of a normal clinician not administrating a vaccine */
  private static double noVaccineProbabilityClinician = 10; // default value
  /** Probability of an antivax clinician not administrating a vaccine */
  private static double noVaccineProbabilityAntivaxClinician = 90; // default value

  private static CodeMap codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap();

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static final Map<String, Map> immunizationSchedule = loadImmunizationSchedule();


  /**
   * NEW METHOD FETCHING IMMUNIZATION FORECASTER RECOMMENDATION
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void performEncounterWithNewCDS(Person person, long encounterDate) {
    /**
     * Reading patient history
     */
    Map<String, List<Long>> immunizationsGiven;
    if (person.attributes.containsKey(IMMUNIZATIONS)) {
      immunizationsGiven = (Map<String, List<Long>>) person.attributes.get(IMMUNIZATIONS);
    } else {
      immunizationsGiven = new HashMap<String, List<Long>>();
      person.attributes.put(IMMUNIZATIONS, immunizationsGiven);
    }

    try {

      /**
       * Querying new CDS
       */
//      System.out.println("---------immunizationRecommendation start--------------");
      ImmunizationRecommendation immunizationRecommendation = queryForecaster(person, encounterDate, immunizationsGiven);
//      System.out.println("---------immunizationRecommendation end--------------");
      if(immunizationRecommendation != null){
//        System.out.println("--------- in checkForCombination--------------");
        // Fetch patient age 
//        System.out.println("Fetch patient age");
        double agePatient= person.ageInDecimalYears(encounterDate); // encounterDate is the current simulation time
//        System.out.println("Patient age : "+ agePatient);
        HashMap<org.mitre.synthea.codebase.generated.Code, NDC> cvxMap = checkForCombination(immunizationRecommendation, encounterDate, agePatient);
//        System.out.println("---------checkForCombination end--------------");
        boolean getImmunization;
        if (!cvxMap.isEmpty()){
//          System.out.println("---------cvxMap is not empty--------------");

//          getImmunization = gettingImmunization(person);
            getImmunization = true;
          System.out.println("Administrated vaccine : ");
          for (Map.Entry<org.mitre.synthea.codebase.generated.Code, NDC> entryMap : cvxMap.entrySet()) {
//            System.out.println("---for cvx = " + entryMap.toString() + "---");
            if (getImmunization) {
              /**
               * getting specific history on cvx, name
               */
              List<Long> history = null;

              org.mitre.synthea.codebase.generated.Code immunizationCode = entryMap.getKey();
              String immunizationKey = codeMap.getStringForCode(immunizationCode, CodesetType.VACCINATION_CVX_CODE);
              String immunizationLabel = immunizationCode.getLabel();
              String ndcCode = cvxMap.get(immunizationCode).getNdcCode();
              String ndcLabel = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE, ndcCode).getLabel();
              System.out.println("CVX : " + immunizationKey + " " + immunizationLabel + " | NDC : " + ndcCode + " " + ndcLabel);
//              System.out.println("Immunization Key : "+ immunizationKey);

              if (immunizationsGiven.containsKey(immunizationKey)) {
                history = immunizationsGiven.get(immunizationKey);
              } else {
                history = new ArrayList<Long>();
                immunizationsGiven.put(immunizationKey, history);
              }
//              System.out.println("Immunization history : " + history);
              history.add(encounterDate);
//              System.out.println("Immunization history after : " + history);
              HealthRecord.Immunization entry = person.record.immunization(encounterDate, immunizationKey);
              HealthRecord.Code immCode = new HealthRecord.Code(
                      "http://hl7.org/fhir/sid/cvx",
                      immunizationKey,
                      immunizationLabel
                      );
              entry.codes.add(immCode);
              entry.series = history.size() + 1;

              // Assign the NDC details to the immunization entry
              entry.nameNDC = ndcLabel;
              entry.codeStringNDC = ndcCode;
            }
          }

        } else {
//          System.out.println("---------cvxMap is empty------------");
        }
      } else {
//        System.err.println("---------------Empty immunization recommendation returned from CDS server.");
      }

    } catch (Exception exception) {
      exception.printStackTrace();
      System.err.println(exception.getMessage());
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  /**
   * NEW METHOD FETCHING IMMUNIZATION FORECASTER RECOMMENDATION
   */
  public static void performEncounterWithForecasterDepreciated(Person person, long time) {
    /**
     * Reading patient history
     */
    Map<String, List<Long>> immunizationsGiven;
    if (person.attributes.containsKey(IMMUNIZATIONS)) {
      immunizationsGiven = (Map<String, List<Long>>) person.attributes.get(IMMUNIZATIONS);
    } else {
      immunizationsGiven = new HashMap<String, List<Long>>();
      person.attributes.put(IMMUNIZATIONS, immunizationsGiven);
    }

    try {
      /**
       * Allows logs to be accessible here
       */
      SoftwareResult softwareResult = new SoftwareResult();
      /**
       * Querying forecaster
       */
      List<ForecastActual> forecastActuals = queryForecasterDepreciated(person,time,immunizationsGiven,softwareResult);
//      System.out.println("Forecast length: " + forecastActuals.size()); TODO remove useless logs
//      System.out.println(softwareResult.getLogText());
//      String log = softwareResult.getLogText().split("VACCINATIONS RECOMMENDED ")[1].split("\nVACCCINATIONS RECOMMENDED AFTER ")[0];
//      log = log.strip();
//      System.out.println(log);
//      System.out.println("log length = " + (log.split("\n").length - 1));
      /**
       * Filtering the result of the forecaster (some vaccines are duplicated)
       */
      List<Integer> vaccineGroupIdList = new ArrayList<>();
      List<String> vaccineCvxList = new ArrayList<>();
      Iterator<ForecastActual> iterator = forecastActuals.iterator();
      while (iterator.hasNext()) {
        ForecastActual forecastActual = iterator.next();
        if (vaccineGroupIdList.contains(forecastActual.getVaccineGroup().getVaccineGroupId())
                || vaccineCvxList.contains(forecastActual.getVaccineGroup().getVaccineCvx())
                || forecastActual.getVaccineGroup().getLabel().equals("DTaP, Tdap or Td")) {
          iterator.remove();
        } else {
          vaccineGroupIdList.add(forecastActual.getVaccineGroup().getVaccineGroupId());
          vaccineCvxList.add(forecastActual.getVaccineGroup().getVaccineCvx());
        }
      };
      vaccineGroupIdList = null;
      vaccineCvxList = null;

      forecastActuals = checkForCombinationDepreciated(forecastActuals);

      Random random;
      int randomNumber;

      for (ForecastActual forecastActual : forecastActuals) {
        /**
         * Filtering Finished forecast, and only when due date is not passed
         * TODO add probability if for early administration : Change date on the immunization ressource or plan an encounter ?
         */
        random = new Random();
        randomNumber = random.nextInt(100);
        if ( forecastActual.getAdminStatus().equals(Admin.FINISHED.getAdminStatus())
                || forecastActual.getAdminStatus().equals(Admin.NOT_RECOMMENDED.getAdminStatus())
                || forecastActual.getAdminStatus().equals(Admin.COMPLETE_FOR_SEASON.getAdminStatus())
                || forecastActual.getDueDate().after(new Date(time + 24*3600))
                || randomNumber < 2) {          // 2% is an arbitrary number for skipping all the immunizations at once.
          break;
        }
//        System.out.println(forecastActual);
//        System.out.println(forecastActual.getVaccineGroup().getLabel() + " cvx code "+ forecastActual.getVaccineGroup().getVaccineCvx() +  " Adminlabel " + forecastActual.getAdmin().getLabel() + " | " + forecastActual.getAdminStatus());
//        System.out.println(forecastActual.getAdmin().toString());
//        System.out.println(i + " EXPLANATION: " + forecastActual.getExplanationHtml());

        /**
         * named immunization in original code
         */
        String immunizationKey = forecastActual.getVaccineGroup().getVaccineCvx();

        random = new Random();
        randomNumber = random.nextInt(100);
        boolean get_immunization = true;

        if (Objects.equals(immunizationKey, "88")) { // for influenza
          if (person.ageInYears(time) >= 65 && randomNumber >= 75) {
            // 75% is the target vaccination coverage by the WHO for older people (https://www.who.int/europe/news-room/fact-sheets/item/influenza-vaccination-coverage-and-effectiveness)
            get_immunization = false;
          } else if (randomNumber >= 15) {
            // 15% is an arbitrary number
            get_immunization = false;
          }
        } else if (randomNumber < 5) { // other immunization
          // 5% is an arbitrary number
          get_immunization = false;
        }

        if (get_immunization) {
          /**
           * getting specific history on cvx, name
           */
          List<Long> history = null;
          if (immunizationsGiven.containsKey(immunizationKey)) {
            history = immunizationsGiven.get(immunizationKey);
          } else {
            history = new ArrayList<Long>();
            immunizationsGiven.put(immunizationKey, history);
          }
          history.add(time);
          HealthRecord.Immunization entry = person.record.immunization(time, immunizationKey);
          HealthRecord.Code immCode = new HealthRecord.Code(
                  "http://hl7.org/fhir/sid/cvx",
                  forecastActual.getVaccineGroup().getVaccineCvx(),
                  forecastActual.getVaccineGroup().getLabel());
          entry.codes.add(immCode);
          entry.series = history.size() + 1;
        }
      }
    } catch (Exception exception) {
      exception.printStackTrace();
      System.err.println(exception.getMessage());
    }
  }

  /**
   * Return a map of CVX codes to NDC codes for the vaccines combination recommended by the CDS that can be administered
   */
  private static HashMap<org.mitre.synthea.codebase.generated.Code, NDC> checkForCombination(ImmunizationRecommendation immunizationRecommendation, long encounterDate, double agePatient) {
    if(!immunizationRecommendation.isEmpty()){
      List<String> combinationVaccines = new ArrayList<>();
      HashMap<org.mitre.synthea.codebase.generated.Code, NDC> cvxMap = new HashMap<>(); // Immunization CVX code to NDC map

      // Put all administrable vaccines in a list
      System.out.println("-----------------------------------------------------------------");
//      System.out.println("Encounter date : " + new Date(encounterDate));
      System.out.print("---Immunization cvx recommended : \n");
      for (ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent recommendation : immunizationRecommendation.getRecommendation()) {
        String immunizationKey = recommendation.getVaccineCode().get(0).getCodingFirstRep().getCode(); // CVX code
        Date dueDate = recommendation.getDateCriterionFirstRep().getValue();  // Recommended due date
        System.out.print(immunizationKey + " " + recommendation.getVaccineCode().get(0).getCodingFirstRep().getDisplay() + " | " + dueDate + " ; \n");


        if (dueDate == null || dueDate.after(new Date(encounterDate))) {
          continue;
        }
        combinationVaccines.add(immunizationKey);
      }
      System.out.println("---");
      System.out.println("Combination vaccines : " + combinationVaccines);
        // Check for combination of vaccines

//      System.out.println("---in getCombosByCVXList---");
      List<Combo> combinations = codeMap.getCombosByCVXStringList(combinationVaccines, agePatient);
//      System.out.println("---end getCombosByCVXList---");

//      if (combinations.isEmpty()) {
//        System.out.println("No combination found");
//        return cvxMap;
//      } else {
//        System.out.println("Combination found");
//      }
      // The first combination is the combination with the best scores
      Combo bestCombination = combinations.get(0);

      // Add the combination to the immunization recommendation
      for (NDC ndc : bestCombination.getNdcList()) {
//        String ndcCode = bestCombination.getNdcList().get(0).getNdcCode();
//        String ndcName = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE, ndc.getNdcCode()).getLabel();
          // Get all cvx relating to the NDC
        for (org.mitre.synthea.codebase.generated.Code cvx : ndc.getCvxCodes()) {
            cvxMap.put(cvx, ndc);
//          System.out.println(" CVX code : " + cvx.getValue() + " CVX name : " + cvx.getLabel() + " NDC code : " + ndc.getNdcCode());
        }

      }
      return cvxMap;
    }
//    System.out.println("Immunization recommendation is empty in checkForCombination");
    return new HashMap<>();
  }

  private static List<ForecastActual> checkForCombinationDepreciated(List<ForecastActual> forecastActualList) {
    List<List<String>> listOfCombinations = new ArrayList<>();
    listOfCombinations.add(List.of("120", "DTaP-IPV-Hib", "20", "10", "48"));
    listOfCombinations.add(List.of("03", "MMR", "05", "07", "06"));
    listOfCombinations.add(List.of("94", "MMRV", "03", "21"));

    List<String> immunizationList = new ArrayList<>();
    for (ForecastActual forecastActual : forecastActualList) {
      immunizationList.add(forecastActual.getVaccineGroup().getVaccineCvx());
    }

    List<Integer> index = new ArrayList<>();
    for (List<String> combinationVaccine : listOfCombinations) {
      index.clear();
      for (String cvxCode : combinationVaccine.subList(2, combinationVaccine.size())) {
        if (immunizationList.contains(cvxCode)) {
          for (String immunization : immunizationList) {
            if (immunization.equals(cvxCode)) {
              index.add(immunizationList.indexOf(immunization));
              break;
            }
          }
        }
        else { break;}
      }

      index.sort((a, b) -> Integer.compare(b, a));

      if (index.size() == combinationVaccine.size() - 2) {
        if (!immunizationList.contains(combinationVaccine.get(0))) {
          ForecastActual newVaccine = new ForecastActual();
          newVaccine.setAdminStatus("N");
          newVaccine.setVaccineGroup(new VaccineGroup(Integer.parseInt(combinationVaccine.get(0)),combinationVaccine.get(1), combinationVaccine.get(0)));
          forecastActualList.add(newVaccine);
        }

        Iterator<Integer> iterator = index.iterator();
        while (iterator.hasNext()) {
          int ind = iterator.next();
          immunizationList.remove(ind);
          forecastActualList.remove(ind);
          iterator.remove();
        }
      }
    }
    for (ForecastActual forecastActual : forecastActualList) {
      System.out.println(forecastActual.getVaccineGroup().getLabel() + " cvx code "+ forecastActual.getVaccineGroup().getVaccineCvx() +  " Adminlabel " + forecastActual.getAdmin().getLabel() + " | " + forecastActual.getAdminStatus());
    }
    return forecastActualList;
  }

  public static ImmunizationRecommendation queryForecaster(Person person, long time, Map<String, List<Long>> immunizationsGiven) throws Exception {
    // This fonction is going to ask the new CDS the Immunization Recommendation of the patient

//        System.out.println("person :" + person);
//        System.out.println("time :" + time);
//        System.out.println("immunizationsGiven :" + immunizationsGiven);

    // Create FHIR context and client
    FhirContext ctx = FhirContext.forR4();

    IGenericClient client = ctx.newRestfulGenericClient(immunizationServer);

    // Build the Parameters resource
    Parameters parameters = new Parameters();

    // Add assessmentDate parameter
    parameters.addParameter()
            .setName("assessmentDate")
            .setValue(new DateType(new Date(time)));

    // Convert patient gender to FHIR format
    String patientGender = person.attributes.get("gender").toString();
    if (patientGender.equals("M")) {
      patientGender = "male";
    } else if (patientGender.equals("F")) {
      patientGender = "female";
    }

    // Create Patient resource
    Patient patient = new Patient();
    patient.setId("example");
    patient.setGender(Enumerations.AdministrativeGender.fromCode((patientGender)));
    patient.setBirthDate(new Date((Long) person.attributes.get("birthdate")));

    // Add patient parameter
    parameters.addParameter()
            .setName("patient")
            .setResource(patient);

    // Add immunization parameters
    for (Map.Entry<String, List<Long>> immunizationEntry : immunizationsGiven.entrySet()) {
      String vaccineCodeStr = immunizationEntry.getKey();
      for (Long eventTime : immunizationEntry.getValue()) {
        Immunization immunization = new Immunization();
        immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);
        immunization.setId("imm-" + eventTime);

        CodeableConcept vaccineCode = new CodeableConcept();
        vaccineCode.addCoding()
                .setSystem("http://hl7.org/fhir/sid/cvx")
                .setCode(vaccineCodeStr);
        immunization.setVaccineCode(vaccineCode);

        immunization.setOccurrence(new DateTimeType(new Date(eventTime)));

        parameters.addParameter()
                .setName("immunization")
                .setResource(immunization);
      }
    }

    // Invoke the $immds-forecast operation
    Parameters out = client
            .operation()
            .onServer()
            .named("$immds-forecast")
            .withParameters(parameters)
            .execute();

    // Fetch the ImmunizationRecommendation from the response
    ImmunizationRecommendation immunizationRecommendation = null;
    for (Parameters.ParametersParameterComponent parameter : out.getParameter()) {
      if (parameter.getName().equals("recommendation") && parameter.hasResource() && parameter.getResource() instanceof ImmunizationRecommendation) {
        immunizationRecommendation = (ImmunizationRecommendation) parameter.getResource();
        break;
      }
    }

    // Return the ImmunizationRecommendation resource
    return immunizationRecommendation;
  }


  private static boolean gettingImmunization(Person person) {
    // Decide whether to administer the vaccine
    Random random = new Random();
    int randomNumber = random.nextInt(100);
    boolean getImmunization = true;

    // If antivax person
    if ((boolean) person.attributes.getOrDefault(Person.ANTIVAX, false)){
      if (randomNumber < noVaccineProbabilityAntivax) {
        getImmunization = false;
      }
    } else {
//            if (Objects.equals(immunizationKey, "88")) { // for influenza
//              if (person.ageInYears(time) >= 65 && randomNumber >= 75) {
//                // 75% is the target vaccination coverage by the WHO for older people (https://www.who.int/europe/news-room/fact-sheets/item/influenza-vaccination-coverage-and-effectiveness)
//                getImmunization = false;
//              } else if (randomNumber >= 15) {
//                // 15% is an arbitrary number
//                getImmunization = false;
//              }
//            } else if (randomNumber < 5) { // other immunization
//              // 5% is an arbitrary number
//              getImmunization = false;
//            }
      if (randomNumber < noVaccineProbability) {
        getImmunization = false;
      }
    }

    // If antivax clinician
    HealthRecord.Encounter currentEncounter = (HealthRecord.Encounter) person.attributes.get(Person.CURRENT_ENCOUNTER);
    randomNumber = random.nextInt(100);
    if ((boolean) currentEncounter.clinician.attributes.getOrDefault(Person.ANTIVAX, false)) {
      if (randomNumber < noVaccineProbabilityAntivaxClinician) {
        getImmunization = false;
      }
    } else {
      if (randomNumber < noVaccineProbabilityClinician) {
        getImmunization = false;
      }
    }
    return getImmunization;
  }

  @SuppressWarnings("rawtypes")
  private static Map loadImmunizationSchedule() {
    String filename = "immunization_schedule.json";
    try {
      String json = Utilities.readResource(filename);
      Gson g = new Gson();
      return g.fromJson(json, HashMap.class);
    } catch (Exception e) {
      System.err.println("ERROR: unable to load json: " + filename);
      e.printStackTrace();
      throw new ExceptionInInitializerError(e);
    }
  }

  private static List<ForecastActual> queryForecasterDepreciated(Person person, long time, Map<String, List<Long>> immunizationsGiven, SoftwareResult softwareResult) throws Exception {
    TestCase testCase = new TestCase();
    testCase.setDateSet(DateSet.FIXED);
    testCase.setEvalDate(new Date(time));
    testCase.setPatientDob(new Date((Long) person.attributes.get("birthdate")));
    testCase.setPatientSex((String) person.attributes.get("gender"));

    /**
     * Giving immunization history to forecaster
     */
    List<TestEvent> testEvents = new ArrayList<>(immunizationsGiven.size());
    testCase.setTestEventList(testEvents);

    int eventId = 0;
    for (Map.Entry<String, List<Long>> immunizationEntry: immunizationsGiven.entrySet()) {
      if (immunizationEntry.getKey().equals("covid19")) {
        break;
      }
      for (Long eventTime: immunizationEntry.getValue()) {
        TestEvent testEvent = new TestEvent();
        Event event = new Event();
        event.setEventId(eventId++);
        event.setVaccineCvx(immunizationEntry.getKey());
        event.setEventType(EventType.VACCINATION);
        testEvent.setEvent(event);
        testEvent.setEventDate(new Date(eventTime));
        testEvents.add(testEvent);
      }
    }

    softwareResult.setTestCase(testCase);

    /**
     * querying forecaster
     */
    Software software = new Software();
    software.setServiceUrl("https://sabbia.westus2.cloudapp.azure.com/lonestar/forecast");
    software.setService(Service.LSVF);
    ConnectorInterface connectorInterface = ConnectFactory.createConnecter(software);
    connectorInterface.setLogText(true);

    return connectorInterface.queryForForecast(testCase,softwareResult);

  }

  /**
   * Administer vaccines to the person at the state time according to the
   * required immunization schedule.
   * @param person - the person to vaccinate.
   * @param encounterDate - the current simulation time.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void performEncounter(Person person, long encounterDate){
    {
      // logging immunization strings to compare and do mapping TODO remove
      Gson g = new Gson();
//      System.out.println("LOGGING IMMUNIZATION History : " + g.toJson(person.attributes.get("gender")) );
//      System.out.println("LOGGING IMMUNIZATION History : " + g.toJson(person.attributes.get(IMMUNIZATIONS)) );
//      System.out.println("LOGGING IMMUNIZATION SCHEDULE keys : " + g.toJson(immunizationSchedule.keySet()) + "\n" );
//      System.out.println("LOGGING All codes " + g.toJson(getAllCodes()) + "\n" );
//      for (String key : person.attributes.keySet()) {
//        System.out.println("LOGGING PERSON ATTRIBUTES  key : " + key );
//      }
    }
    /**
     * New code connecting to forecaster
     */
    performEncounterWithNewCDS(person,encounterDate);
    /**
     * old code
     */
//    performEncounterDeprecated(person,time);
  }

  public static void performEncounterDeprecated(Person person, long time){
    Map<String, List<Long>> immunizationsGiven;
    if (person.attributes.containsKey(IMMUNIZATIONS)) {
      immunizationsGiven = (Map<String, List<Long>>) person.attributes.get(IMMUNIZATIONS);
    } else {
      immunizationsGiven = new HashMap<String, List<Long>>();
      person.attributes.put(IMMUNIZATIONS, immunizationsGiven);
    }

    for (String immunization : immunizationSchedule.keySet()) {
      int series = immunizationDue(immunization, person, time, immunizationsGiven);
      if (series > 0) {
        List<Long> history = immunizationsGiven.get(immunization);
        history.add(time);
        HealthRecord.Immunization entry = person.record.immunization(time, immunization);
        Map code = (Map) immunizationSchedule.get(immunization).get("code");
        HealthRecord.Code immCode = new HealthRecord.Code(code.get("system").toString(),
                code.get("code").toString(), code.get("display").toString());
        entry.codes.add(immCode);
        entry.series = series;
      }
    }
  }


  /**
   * Return whether or not the specified immunization is due.
   *
   * @param immunization The immunization to give
   * @param person The person to receive the immunization
   * @param time The time the immunization would be given
   * @param immunizationsGiven The history of immunizations
   * @return -1 if the immunization should not be given, otherwise a positive integer,
   *     where the value is the series. For example, 1 if this is the first time the
   *     vaccine was administered; 2 if this is the second time, et cetera.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static int immunizationDue(String immunization, Person person, long time,
                                    Map<String, List<Long>> immunizationsGiven) {
    int ageInMonths = person.ageInMonths(time);

    List<Long> history = null;
    if (immunizationsGiven.containsKey(immunization)) {
      history = immunizationsGiven.get(immunization);
    } else {
      history = new ArrayList<Long>();
      immunizationsGiven.put(immunization, history);
    }

    // Don't administer if the immunization wasn't historically available at the date of the
    // encounter
    Map schedule = immunizationSchedule.get(immunization);
    Double firstAvailable = (Double) schedule.getOrDefault("first_available", 1900);
    if (time < Utilities.convertCalendarYearsToTime(firstAvailable.intValue())) {
      return -1;
    }

    // Don't administer if all recommended doses have already been given
    List atMonths = new ArrayList((List) schedule.get("at_months"));
    if (history.size() >= atMonths.size()) {
      return -1;
    }

    // See if the patient should receive a dose based on their current age and the recommended dose
    // ages;
    // we can't just see if greater than the recommended age for the next dose they haven't received
    // because i.e. we don't want to administer the HPV vaccine to someone who turns 90 in 2006 when
    // the
    // vaccine is released; we can't just use a simple test of, say, within 4 years after the
    // recommended
    // age for the next dose they haven't received because i.e. PCV13 is given to kids and seniors
    // but was
    // only available starting in 2010, so a senior in 2012 who has never received a dose should get
    // one,
    // but only one; what we do is:

    // 1) eliminate any recommended doses that are not within 4 years of the patient's age
    // at_months = at_months.reject { |am| age_in_months - am >= 48 }
    Predicate<Double> notWithinFourYears = p -> ((ageInMonths - p) >= 48);
    atMonths.removeIf(notWithinFourYears);
    if (atMonths.isEmpty()) {
      return -1;
    }

    // 2) eliminate recommended doses that were actually administered
    for (Long date : history) {
      int ageAtDate = person.ageInMonths(date);
      double recommendedAge = (double) atMonths.get(0);
      if (ageAtDate >= recommendedAge && ((ageAtDate - recommendedAge) < 48)) {
        atMonths.remove(0);
        if (atMonths.isEmpty()) {
          return -1;
        }
      }
    }

    // 3) see if there are any recommended doses remaining that this patient is old enough for
    if (!atMonths.isEmpty() && ageInMonths >= (double) atMonths.get(0)) {
      return history.size() + 1;
    }
    return -1;
  }

  /**
   * Get all of the Codes this module uses, for inventory purposes.
   *
   * @return Collection of all codes and concepts this module uses
   */
  @SuppressWarnings("rawtypes")
  public static Collection<Code> getAllCodes() {
    List<Map> rawCodes = (List<Map>) immunizationSchedule.values()
            .stream().map(m -> (Map)m.get("code")).collect(Collectors.toList());

    List<Code> convertedCodes = new ArrayList<Code>(rawCodes.size());

    for (Map m : rawCodes) {
      Code immCode = new Code(m.get("system").toString(),
              m.get("code").toString(),
              m.get("display").toString());

      convertedCodes.add(immCode);
    }

    return convertedCodes;
  }

  /**
   * Get the maximum number of vaccine doses for a particular code.
   * @param code The vaccine code.
   * @return The maximum number of doses to be administered.
   */
  @SuppressWarnings("rawtypes")
  public static int getMaximumDoses(String code) {
    for (String immunization : immunizationSchedule.keySet()) {
      Map icode = (Map) immunizationSchedule.get(immunization).get("code");
      if (icode.get("code").equals(code)) {
        List doses = (List) immunizationSchedule.get(immunization).get("at_months");
        return doses.size();
      }
    }
    return 1;
  }

  /**
   * Populate the given attribute map with the list of attributes that this
   * module reads/writes with example values when appropriate.
   *
   * @param attributes Attribute map to populate.
   */
  public static void inventoryAttributes(Map<String,Inventory> attributes) {
    String m = Immunizations.class.getSimpleName();
    // Read & Write
    Attributes.inventory(attributes, m, IMMUNIZATIONS, true, true, "Map<String, List<Long>>");
  }

  public static void setImmunizationServer(String immunizationServer) {
    Immunizations.immunizationServer = immunizationServer;
  }

  public static void setNoVaccineProbability(double noVaccineProbability) {
    Immunizations.noVaccineProbability = noVaccineProbability;
  }

  public static double getNoVaccineProbability() {
    return Immunizations.noVaccineProbability;
  }

  public static double getNoVaccineProbabilityAntivax() {
    return Immunizations.noVaccineProbabilityAntivax;
  }

  public static void setNoVaccineProbabilityAntivax(double noVaccineProbabilityAntivax) {
    Immunizations.noVaccineProbabilityAntivax = noVaccineProbabilityAntivax;
  }

  public static void setNoVaccineProbabilityClinician(double noVaccineProbabilityClinician) {
    Immunizations.noVaccineProbabilityClinician = noVaccineProbabilityClinician;
  }

  public static void setNoVaccineProbabilityAntivaxClinician(double noVaccineProbabilityAntivaxClinician) {
    Immunizations.noVaccineProbabilityAntivaxClinician = noVaccineProbabilityAntivaxClinician;
  }
}
