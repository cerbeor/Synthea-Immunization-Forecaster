package org.mitre.synthea.modules;

import com.google.gson.Gson;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.immregistries.vfa.connect.ConnectFactory;
import org.immregistries.vfa.connect.ConnectorInterface;
import org.immregistries.vfa.connect.model.*;
import org.mitre.synthea.helpers.Attributes;
import org.mitre.synthea.helpers.Attributes.Inventory;
import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.Code;

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

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static final Map<String, Map> immunizationSchedule = loadImmunizationSchedule();


  @SuppressWarnings({ "unchecked", "rawtypes" })
  /**
   * ONLY NEW METHOD FETCHING IMMUNIZATION FORECASTER RECOMMENDATION
   * TODO MAP objects
   */
  public static void performEncounterWithForecaster(Person person, long time) {


    TestCase testCase = new TestCase();
    testCase.setDateSet(DateSet.FIXED);
    testCase.setEvalDate(new Date(time));
    testCase.setPatientDob(new Date((Long) person.attributes.get("birthdate")));
    testCase.setPatientSex("M"); // TODO read from attributes

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

    List<TestEvent> testEvents = new ArrayList<>(immunizationsGiven.size());
    testCase.setTestEventList(testEvents);
    for (Map.Entry<String, List<Long>> immunizationEntry: immunizationsGiven.entrySet()) {
      for (Long eventTime: immunizationEntry.getValue()) {
        TestEvent testEvent = new TestEvent();
        Event event = new Event();
        event.setVaccineCvx(immunizationEntry.getKey());
        testEvent.setEvent(event);
        testEvent.setEventDate(new Date(eventTime));
        testEvents.add(testEvent);
      }
    }

    SoftwareResult softwareResult = new SoftwareResult();
    softwareResult.setTestCase(testCase);
    List<ForecastActual> forecastActuals;
    try {
      /**
       * querying forecaster
       */
      Software software = new Software();
      software.setServiceUrl("https://sabbia.westus2.cloudapp.azure.com/lonestar/forecast");
      software.setService(Service.LSVF);
      ConnectorInterface connectorInterface =  ConnectFactory.createConnecter(software);
      connectorInterface.setLogText(true);

      forecastActuals = connectorInterface.queryForForecast(testCase,softwareResult);
      /**
       * currently getting empty results, TODO investigate
       */
//      System.out.println(forecastActuals.size());
//      System.out.println(softwareResult.getSoftwareResultStatus());
      System.out.println(softwareResult.getLogText());
//      System.out.println(softwareResult);
//      System.out.println(softwareResult.getIssueList().get(0));


      for (ForecastActual forecastActual : forecastActuals) {
        /**
         * named immunization in original code
         */
        String immunizationKey = forecastActual.getVaccineCvx(); // TODO take actual synthea immunization key

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
        if(immunizationSchedule.get(immunizationKey) == null) {
          immunizationSchedule.put(immunizationKey, new HashMap<>());
        }
        Map code = (Map) immunizationSchedule.get(immunizationKey).get("code");

        if (code != null) {
          HealthRecord.Code immCode = new HealthRecord.Code(code.get("system").toString(),
                  code.get("code").toString(), code.get("display").toString());
          entry.codes.add(immCode);
          entry.series = history.size() + 1;
        }
      }
    } catch (Exception exception) {
      exception.printStackTrace();
      System.err.println(exception.getMessage());
    }
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

  /**
   * Administer vaccines to the person at the state time according to the
   * required immunization schedule.
   * @param person - the person to vaccinate.
   * @param time - the current simulation time.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void performEncounter(Person person, long time){
    {
      // logging immunization strings to compare and do mapping TODO remove
      Gson g = new Gson();
      System.out.println("LOGGING IMMUNIZATION History : " + g.toJson(person.attributes.get(IMMUNIZATIONS)) );
//      System.out.println("LOGGING IMMUNIZATION SCHEDULE : " + g.toJson(immunizationSchedule.get("hib")) + "\n" );
//      for (String key : person.attributes.keySet()) {
//        System.out.println("LOGGING PERSON ATTRIBUTES  key : " + key + "\n" );
//      }
    }
    /**
     * New code connecting to forecaster
     */
    performEncounterWithForecaster(person,time);

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
}
