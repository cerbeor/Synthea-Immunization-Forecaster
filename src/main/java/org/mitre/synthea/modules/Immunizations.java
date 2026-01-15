package org.mitre.synthea.modules;

import com.google.gson.Gson;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.*;
import org.mitre.synthea.codebase.CodeMap;
import org.mitre.synthea.codebase.CodeMapBuilder;
import org.mitre.synthea.codebase.mapping.Combo;
import org.mitre.synthea.codebase.mapping.NDC;
import org.mitre.synthea.codebase.reference.CodesetType;
import org.mitre.synthea.helpers.Attributes;
import org.mitre.synthea.helpers.Attributes.Inventory;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.agents.Provider.ProviderType;
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
  private static double noVaccineProbability = 0; // default value
  /** Probability of an hesitant person not taking a vaccine */
  private static double noVaccineProbabilityHesitantPatient = 100; // default value
  /** Probability of a normal clinician not administrating a vaccine */
  private static double noVaccineProbabilityClinician = 0; // default value
  /** Probability of an under vaxxed clinician not administrating a vaccine */
  private static double noVaccineProbabilityUnderVaxxedClinician = 100; // default value
  /** Flag to use NIST immunization module */
  private static boolean usingNistImmunizationModule = false; // default value : use Synthea's immunization module

  /** CodeMap object for vaccines combination check */
  private static CodeMap codeMap = CodeMapBuilder.INSTANCE.getDefaultCodeMap();

  // Toggle and defaults for synthesizing foreign immunization events.
  private static boolean isForeignImmunizationsEnabled() {
    return Config.getAsBoolean("generate.immunizations.foreign.enabled", false);
  }

  private static String getForeignImmunizationCountry() {
    return Config.get("generate.immunizations.foreign.default_country", "CN");
  }

  private static double getForeignImmunizationProbability() {
    return Config.getAsDouble("generate.immunizations.foreign.probability", 0.0);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static final Map<String, Map> immunizationSchedule = loadImmunizationSchedule();

  /**
   * Simulates an encounter with the NIST (National Institute of Standards and Technology)
   * Clinical Decision Support (CDS) system for immunization recommendations.
   *
   * @param person Person object representing the patient undergoing the encounter.
   *               Contains demographic data, medical history, and attributes.
   * @param encounterDate The simulation time (in milliseconds since epoch) when the encounter occurs.
   *                      Used to determine the patient's age and assess immunization due dates.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void performEncounterWithNistCDS(Person person, long encounterDate,
      Map<String, List<Long>> immunizationsGiven) {

    try {
      // Check if the patient should receive an immunization
      boolean getImmunization = gettingImmunization(person);
      if (getImmunization) {
        // Querying the NIST CDS system for immunization recommendations
        ImmunizationRecommendation immunizationRecommendation = queryForecaster(person, encounterDate, immunizationsGiven);
        if(immunizationRecommendation != null){
          // Fetch patient age
          double agePatient = person.ageInDecimalYears(encounterDate);
          // Check for combination of vaccines
          HashMap<org.mitre.synthea.codebase.generated.Code, NDC> cvxMap = checkForCombination(immunizationRecommendation, encounterDate, agePatient);
          // Check if the patient should receive vaccines
          //System.err.println("immunization key: ");
          if (!cvxMap.isEmpty()){
            // For all vaccines that have to be administered
            for (Map.Entry<org.mitre.synthea.codebase.generated.Code, NDC> entryMap : cvxMap.entrySet()) {

              // Get the immunization details
              List<Long> history = null;
              org.mitre.synthea.codebase.generated.Code immunizationCode = entryMap.getKey();
              String immunizationKey = codeMap.getStringForCode(immunizationCode, CodesetType.VACCINATION_CVX_CODE);
              String immunizationLabel = immunizationCode.getLabel();
              String ndcCode = cvxMap.get(immunizationCode).getNdcCode();
              String ndcLabel = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE_UNIT_OF_USE, ndcCode).getLabel();

                // Add the immunization to the patient's history
              if (immunizationsGiven.containsKey(immunizationKey)) {
                history = immunizationsGiven.get(immunizationKey);
              } else {
                history = new ArrayList<Long>();
                immunizationsGiven.put(immunizationKey, history);
              }
              history.add(encounterDate);
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

              // Assign the NUVA details to the immunization entry
              CvxNuvaMap.Mapping nuvaMapping = CvxNuvaMap.findByCvx(immunizationKey);
              // System.err.println("immunization key: ");
              // System.err.println(immunizationKey);
              if (nuvaMapping != null) {
                entry.nuvaCode = nuvaMapping.getNuvaCode();
                entry.nuvaLabel = nuvaMapping.getLabel();
              }
              else {
                System.err.println("no nuva mapping");
              }
            }
          }
        }
      }

    } catch (Exception exception) {
      exception.printStackTrace();
      System.err.println(exception.getMessage());
    }
  }

  /**
   * Determines the best combination of vaccines to administer based on the recommendations
   * provided by the Clinical Decision Support (CDS) system and the patient's current encounter.
   *
   * @param immunizationRecommendation The ImmunizationRecommendation object containing
   *                                   vaccine recommendations for the patient.
   * @param encounterDate The simulation time (in milliseconds since epoch) of the current encounter.
   *                      Used to filter vaccines that are due for administration.
   * @param agePatient The age of the patient (in decimal years) at the time of the encounter.
   *                   Used to determine eligible vaccine combinations.
   *
   * @return A HashMap mapping CVX codes to NDC codes for vaccines that can be administered.
   *         Returns an empty map if no valid recommendations or combinations are found.
   */
  private static HashMap<org.mitre.synthea.codebase.generated.Code, NDC> checkForCombination(ImmunizationRecommendation immunizationRecommendation, long encounterDate, double agePatient) {
    // Check if the immunization recommendation is not empty
    if(!immunizationRecommendation.isEmpty()){
      List<String> combinationVaccines = new ArrayList<>();
      HashMap<org.mitre.synthea.codebase.generated.Code, NDC> cvxMap = new HashMap<>(); // Immunization CVX code to NDC map

      // Put all administrable vaccines in a list
      for (ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent recommendation : immunizationRecommendation.getRecommendation()) {
        String immunizationKey = recommendation.getVaccineCode().get(0).getCodingFirstRep().getCode(); // CVX code
        Date dueDate = recommendation.getDateCriterionFirstRep().getValue();  // Recommended due date

        // Skip vaccines that are not due for administration
        if (dueDate == null || dueDate.after(new Date(encounterDate))) {
          continue;
        }
        combinationVaccines.add(immunizationKey);
      }

      // Check for combination of vaccines
      List<Combo> combinations = codeMap.getCombosByCVXStringList(combinationVaccines, agePatient);

      // The first combination is the combination with the best scores
      Combo bestCombination = combinations.get(0);

      // Add the combination to the immunization recommendation
      for (NDC ndc : bestCombination.getNdcList()) {

         // Get all cvx relating to the NDC
        for (org.mitre.synthea.codebase.generated.Code cvx : ndc.getCvxCodes()) {
            cvxMap.put(cvx, ndc);
        }

      }
      return cvxMap;
    }
    return new HashMap<>();
  }


  /**
   * This method queries the new Clinical Decision Support (CDS) system to retrieve 
   * an Immunization Recommendation for a given patient based on their immunization history 
   * and demographic details. The method constructs and sends an operation request 
   * to a FHIR server and processes the response.
   *
   * @param person            The patient for whom the immunization recommendation is queried.
   * @param encounterDate     The date of the current encounter or assessment in milliseconds.
   * @param immunizationsGiven A map of immunization history, where the key is the vaccine 
   *                           code (CVX) and the value is a list of administration timestamps.
   * @return ImmunizationRecommendation object containing the immunization forecast for the patient.
   * @throws Exception If there is an error in the communication with the FHIR server or
   *                   during the construction or parsing of the request/response.
   */
  public static ImmunizationRecommendation queryForecaster(Person person, long encounterDate, Map<String, List<Long>> immunizationsGiven) throws Exception {
    // This fonction is going to ask the new CDS the Immunization Recommendation of the patient

    // Create FHIR context and client
    // Initialize the FHIR context for R4 and create a client to communicate with the FHIR server
    FhirContext ctx = FhirContext.forR4();
    IGenericClient client = ctx.newRestfulGenericClient(immunizationServer);

    // Construct a FHIR Parameters resource to hold the inputs for the CDS operation
    Parameters parameters = new Parameters();

    // Add the assessment date to the Parameters resource
    parameters.addParameter()
            .setName("assessmentDate")
            .setValue(new DateType(new Date(encounterDate)));

    // Retrieve and convert the patient's gender to match FHIR's gender codes
    String patientGender = person.attributes.get("gender").toString();
    if (patientGender.equals("M")) {
        patientGender = "male";
    } else if (patientGender.equals("F")) {
        patientGender = "female";
    }

    // Construct a FHIR Patient resource using the patient's demographic information
    Patient patient = new Patient();
    patient.setId("example"); // Example ID for the patient
    patient.setGender(Enumerations.AdministrativeGender.fromCode(patientGender));
    patient.setBirthDate(new Date((Long) person.attributes.get("birthdate")));

    // Add the Patient resource to the Parameters resource
    parameters.addParameter()
            .setName("patient")
            .setResource(patient);

    // Iterate over the immunization history and construct FHIR Immunization resources
    for (Map.Entry<String, List<Long>> immunizationEntry : immunizationsGiven.entrySet()) {
        String vaccineCodeStr = immunizationEntry.getKey();
        for (Long eventTime : immunizationEntry.getValue()) {
            Immunization immunization = new Immunization();
            immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED); // Immunization status
            immunization.setId("imm-" + eventTime); // Unique identifier for the immunization event

            // Set the vaccine code using the CVX code system
            CodeableConcept vaccineCode = new CodeableConcept();
            vaccineCode.addCoding()
                    .setSystem("http://hl7.org/fhir/sid/cvx")
                    .setCode(vaccineCodeStr);
            immunization.setVaccineCode(vaccineCode);

            // Set the occurrence date of the immunization
            immunization.setOccurrence(new DateTimeType(new Date(eventTime)));

            // Add the Immunization resource to the Parameters resource
            parameters.addParameter()
                    .setName("immunization")
                    .setResource(immunization);
        }
    }

    // Perform the $immds-forecast operation on the FHIR server
    Parameters out = client
            .operation()
            .onServer()
            .named("$immds-forecast")
            .withParameters(parameters)
            .execute();

    // Extract the ImmunizationRecommendation resource from the operation response
    ImmunizationRecommendation immunizationRecommendation = null;
    for (Parameters.ParametersParameterComponent parameter : out.getParameter()) {
        if (parameter.getName().equals("recommendation") && parameter.hasResource() && parameter.getResource() instanceof ImmunizationRecommendation) {
            immunizationRecommendation = (ImmunizationRecommendation) parameter.getResource();
            break;
        }
    }

    // Return the ImmunizationRecommendation resource to the caller
    return immunizationRecommendation;
  }

  /**
   * Determines whether a vaccine should be administered to the patient during the current encounter,
   * considering factors such as patient preferences, clinician preferences, and random probability.
   *
   * @param person The Person object representing the patient. The method uses attributes
   *               from the person object to determine hesitant status and retrieve encounter details.
   *
   * @return true if the vaccine should be administered; false otherwise.
   */

  private static boolean gettingImmunization(Person person) {

    // Generate a random number to determine whether the patient should receive the vaccine
    Random random = new Random();
    int randomNumber = random.nextInt(100);
    // Default value for the immunization
    boolean getImmunization = true;

    // If hesitant person
    if ((boolean) person.attributes.get(Person.HESITANT_PATIENT)){
      if (randomNumber < noVaccineProbabilityHesitantPatient) {
        getImmunization = false;
      }
    } else {
      if (randomNumber < noVaccineProbability) {
        getImmunization = false;
      }
    }

    // If under vaxxed clinician
    HealthRecord.Encounter currentEncounter = (HealthRecord.Encounter) person.attributes.get(Person.CURRENT_ENCOUNTER);
    // Generate a random number to determine whether the clinician should administer the vaccine
    randomNumber = random.nextInt(100);
    // If the clinician is hesitant
    if ((boolean) currentEncounter.clinician.attributes.get(Person.HESITANT_PATIENT)) {
      if (randomNumber < noVaccineProbabilityUnderVaxxedClinician) {
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

  // Apply NUVA mapping to the immunization entry when a CVX mapping exists.
  private static void addNuvaCoding(HealthRecord.Immunization entry, String cvxCode) {
    CvxNuvaMap.Mapping nuvaMapping = CvxNuvaMap.findByCvx(cvxCode);
    if (nuvaMapping != null) {
      entry.nuvaCode = nuvaMapping.getNuvaCode();
      entry.nuvaLabel = nuvaMapping.getLabel();
    }
  }

  // Build a fixed set of foreign vaccine options keyed by country for travel scenarios.
  private static List<ForeignVaccineOption> buildForeignVaccineOptions(String destinationCountry) {
    if (destinationCountry == null || destinationCountry.isEmpty()) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(defaultForeignVaccines());
  }

  // Default set of vaccines that can be administered abroad when enabled.
  private static List<ForeignVaccineOption> defaultForeignVaccines() {
    List<ForeignVaccineOption> vaccines = new ArrayList<>();
    addForeignOption(vaccines, "41", "Typhoid polysaccharide vaccine, unspecified");
    addForeignOption(vaccines, "56", "Dengue vaccine, unspecified");
    addForeignOption(vaccines, "188", "Shingles vaccine, unspecified");
    return vaccines;
  }

  private static final int TRAVEL_START_MIN_DAYS = 1;
  private static final int TRAVEL_START_MAX_DAYS = 14;
  private static final int TRAVEL_DURATION_MIN_DAYS = 3;
  private static final int TRAVEL_DURATION_MAX_DAYS = 21;
  private static final int MAX_TRAVEL_VACCINES = 3;
  private static final double MULTI_VACCINATION_TRAVEL_PROBABILITY = 0.45;
  private static final long TRAVEL_IMMUNIZATION_MIN_SPACING =
      TimeUnit.DAYS.toMillis(1);

  private static int getTravelStartMinDays() {
    return Config.getAsInteger("generate.immunizations.foreign.travel.start_days.min",
        TRAVEL_START_MIN_DAYS);
  }

  private static int getTravelStartMaxDays() {
    return Config.getAsInteger("generate.immunizations.foreign.travel.start_days.max",
        TRAVEL_START_MAX_DAYS);
  }

  private static int getTravelDurationMinDays() {
    return Config.getAsInteger("generate.immunizations.foreign.travel.duration_days.min",
        TRAVEL_DURATION_MIN_DAYS);
  }

  private static int getTravelDurationMaxDays() {
    return Config.getAsInteger("generate.immunizations.foreign.travel.duration_days.max",
        TRAVEL_DURATION_MAX_DAYS);
  }

  private static int getTravelMaxVaccines() {
    return Config.getAsInteger("generate.immunizations.foreign.travel.vaccine.max_count",
        MAX_TRAVEL_VACCINES);
  }

  private static double getTravelMultiVaccineProbability() {
    return Config.getAsDouble("generate.immunizations.foreign.travel.vaccine.multi_probability",
        MULTI_VACCINATION_TRAVEL_PROBABILITY);
  }


  // Populate an option, preferring NUVA labels when present for clarity.
  private static void addForeignOption(List<ForeignVaccineOption> vaccines, String cvxCode,
      String fallbackLabel) {
    String label = fallbackLabel;
    CvxNuvaMap.Mapping mapping = CvxNuvaMap.findByCvx(cvxCode);
    if (mapping != null && mapping.getLabel() != null && !mapping.getLabel().isEmpty()) {
      label = mapping.getLabel();
    }
    vaccines.add(new ForeignVaccineOption(cvxCode, label));
  }

    private static long randomTravelStartOffset(Person person) {
    int minDays = getTravelStartMinDays();
    int maxDays = Math.max(minDays, getTravelStartMaxDays());
    int span = maxDays - minDays + 1;
    int days = minDays + person.randInt(span);
    return TimeUnit.DAYS.toMillis(days);
  }

  private static long randomTravelDuration(Person person) {
    int minDays = getTravelDurationMinDays();
    int maxDays = Math.max(minDays, getTravelDurationMaxDays());
    int span = maxDays - minDays + 1;
    int days = minDays + person.randInt(span);
    return TimeUnit.DAYS.toMillis(days);
  }

  private static long findTravelImmunizationTime(Person person, long travelStart,
      long travelEnd, Map<String, List<Long>> immunizationsGiven) {
    long travelWindow = Math.max(0, travelEnd - travelStart);
    long travelHours = Math.max(1, TimeUnit.MILLISECONDS.toHours(travelWindow));
    int attempts = Math.max(10, (int) travelHours * 2);
    long selectedTime = travelStart;
    for (int attempt = 0; attempt < attempts; attempt++) {
      long candidateTime = travelStart + TimeUnit.HOURS.toMillis(person.randInt((int) travelHours));
      if (isTravelTimeSpaced(candidateTime, immunizationsGiven)) {
        return candidateTime;
      }
      selectedTime = candidateTime;
    }
    return selectedTime;
  }

  private static boolean isTravelTimeSpaced(long candidateTime,
      Map<String, List<Long>> immunizationsGiven) {
    for (List<Long> history : immunizationsGiven.values()) {
      for (Long time : history) {
        if (Math.abs(candidateTime - time) < TRAVEL_IMMUNIZATION_MIN_SPACING) {
          return false;
        }
      }
    }
    return true;
  }

  private static int travelVaccineCount(Person person, int optionsCount) {
    int maxConfigured = Math.max(1, getTravelMaxVaccines());
    int maxOptions = Math.min(optionsCount, maxConfigured);
    if (maxOptions <= 1) {
      return 1;
    }
    double probability = getTravelMultiVaccineProbability();
    if (probability < 0) {
      probability = 0;
    } else if (probability > 1) {
      probability = 1;
    }
    if (person.rand() < probability) {
      return 2 + person.randInt(maxOptions - 1);
    }
    return 1;
  }

  private static List<ForeignVaccineOption> selectTravelVaccines(Person person,
      List<ForeignVaccineOption> options) {
    int count = travelVaccineCount(person, options.size());
    List<ForeignVaccineOption> choices = new ArrayList<>(options);
    Collections.shuffle(choices, new Random(person.randInt()));
    return choices.subList(0, Math.min(count, choices.size()));
  }

  @SuppressWarnings("rawtypes")
  private static void maybeGenerateForeignImmunization(Person person, long time,
      Map<String, List<Long>> immunizationsGiven) {
    String destinationCountry = getForeignImmunizationCountry();
    double probability = getForeignImmunizationProbability();
    if (!isForeignImmunizationsEnabled()
        || probability <= 0
        || person.rand() >= probability) {
      return;
    }

    // Randomly select a destination-specific vaccine based on configured options.
    List<ForeignVaccineOption> options = buildForeignVaccineOptions(destinationCountry);
    if (options.isEmpty()) {
      return;
    }

    long travelStart = time + randomTravelStartOffset(person);
    long travelDuration = randomTravelDuration(person);
    long travelEnd = travelStart + travelDuration;

    // Create a synthetic travel encounter to anchor the foreign immunization.
    HealthRecord.Encounter travelEncounter =
        person.record.encounterStart(travelStart, HealthRecord.EncounterType.OUTPATIENT);
    travelEncounter.name = "Travel Encounter - " + destinationCountry;
    travelEncounter.provider = buildForeignProvider(destinationCountry);
    travelEncounter.reason = new Code("http://snomed.info/sct", "171149006",
        "Travel vaccination");

    List<ForeignVaccineOption> selections = selectTravelVaccines(person, options);
    for (ForeignVaccineOption selection : selections) {
      long immunizationTime = findTravelImmunizationTime(person, travelStart, travelEnd,
          immunizationsGiven);
      List<Long> history = immunizationsGiven.computeIfAbsent(
          selection.getCvxCode(), k -> new ArrayList<Long>());
      history.add(immunizationTime);

      // Record the immunization with foreign context identifiers for exporters.
      HealthRecord.Immunization entry = person.record.immunization(immunizationTime,
          selection.getCvxCode());
      entry.codes.add(new HealthRecord.Code("http://hl7.org/fhir/sid/cvx",
          selection.getCvxCode(), selection.getDisplay()));
      entry.series = history.size();
      entry.administeringCountry = destinationCountry;
      entry.administeringOrganizationId = buildForeignOrganizationId(person, destinationCountry);
      entry.administeringLocationId = buildForeignLocationId(person, destinationCountry);
      entry.travelNote = "Immunization administered during travel to "
          + destinationCountry + ".";
      addNuvaCoding(entry, selection.getCvxCode());
    }

    travelEncounter.end(travelEnd);
  }

  // Create a placeholder provider object to represent a foreign administering organization.
  private static Provider buildForeignProvider(String destinationCountry) {
    Provider provider = new Provider();
    provider.name = destinationCountry + " Travel Clinic";
    provider.address = "1 International Way";
    provider.city = "Travel City";
    provider.state = destinationCountry;
    provider.zip = "000000";
    provider.phone = "+0-000-000-0000";
    provider.type = ProviderType.PRIMARY;
    provider.institutional = false;
    provider.servicesProvided.add(HealthRecord.EncounterType.OUTPATIENT);
    provider.getLonLat().setLocation(0.0, 0.0);
    provider.attributes.put("country_code", destinationCountry);
    return provider;
  }

  // Generate (and cache) a deterministic-looking foreign organization identifier per person.
  private static String buildForeignOrganizationId(Person person, String destinationCountry) {
    String key = "foreign_org_" + destinationCountry;
    if (person.attributes.containsKey(key)) {
      return (String) person.attributes.get(key);
    }
    String id = "org-" + destinationCountry.toLowerCase() + "-"
        + person.randUUID().toString();
    person.attributes.put(key, id);
    return id;
  }

  // Generate (and cache) a deterministic-looking foreign location identifier per person.
  private static String buildForeignLocationId(Person person, String destinationCountry) {
    String key = "foreign_location_" + destinationCountry;
    if (person.attributes.containsKey(key)) {
      return (String) person.attributes.get(key);
    }
    String id = "loc-" + destinationCountry.toLowerCase() + "-"
        + person.randUUID().toString();
    person.attributes.put(key, id);
    return id;
  }

  private static final class ForeignVaccineOption {
    private final String cvxCode;
    private final String display;

    private ForeignVaccineOption(String cvxCode, String display) {
      this.cvxCode = cvxCode;
      this.display = display;
    }

    String getCvxCode() {
      return cvxCode;
    }

    String getDisplay() {
      return display;
    }
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
      Gson g = new Gson();
    }

    Map<String, List<Long>> immunizationsGiven = getOrCreateImmunizationsGiven(person);

    // Check if the NIST immunization module is used
    if (usingNistImmunizationModule){
      performEncounterWithNistCDS(person, encounterDate, immunizationsGiven);
      // Allow foreign travel immunizations even when using the NIST forecaster.
      maybeGenerateForeignImmunization(person, encounterDate, immunizationsGiven);
    } else {
      performEncounterSyntheaVersion(person, encounterDate, immunizationsGiven);
    }
  }

  /**
   * Administer vaccines to the person at the state time according to the
   * required immunization schedule.
   * @param person - the person to vaccinate.
   * @param time - the current simulation time.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void performEncounterSyntheaVersion(Person person, long time,
      Map<String, List<Long>> immunizationsGiven){
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
        addNuvaCoding(entry, immCode.code);
      }
    }
    maybeGenerateForeignImmunization(person, time, immunizationsGiven);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, List<Long>> getOrCreateImmunizationsGiven(Person person) {
    if (person.attributes.containsKey(IMMUNIZATIONS)) {
      return (Map<String, List<Long>>) person.attributes.get(IMMUNIZATIONS);
    }
    Map<String, List<Long>> immunizationsGiven = new HashMap<String, List<Long>>();
    person.attributes.put(IMMUNIZATIONS, immunizationsGiven);
    return immunizationsGiven;
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

  public static double getNoVaccineProbabilityHesitantPatient() {
    return Immunizations.noVaccineProbabilityHesitantPatient;
  }

  public static void setNoVaccineProbabilityHesitantPatient(double noVaccineProbabilityHesitantPatient) {
    Immunizations.noVaccineProbabilityHesitantPatient = noVaccineProbabilityHesitantPatient;
  }

  public static void setNoVaccineProbabilityClinician(double noVaccineProbabilityClinician) {
    Immunizations.noVaccineProbabilityClinician = noVaccineProbabilityClinician;
  }

  public static void setNoVaccineProbabilityUnderVaxxedClinician(double noVaccineProbabilityUnderVaxxedClinician) {
    Immunizations.noVaccineProbabilityUnderVaxxedClinician = noVaccineProbabilityUnderVaxxedClinician;
  }

  public static void setUsingNistImmunizationModule(boolean usingNistImmunizationModule) {
    Immunizations.usingNistImmunizationModule = usingNistImmunizationModule;
  }
}
