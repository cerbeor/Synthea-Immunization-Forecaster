package org.mitre.synthea.modules;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.mitre.synthea.world.agents.Person;


public class Immunizations2 {
    public static ImmunizationRecommendation queryForecaster2(Person person, long time, Map<String, List<Long>> immunizationsGiven) throws Exception {
        // This fonction is going to ask the new CDS the Immunization Recommendation of the patient 

        System.out.println("person :" + person);
        System.out.println("time :" + time);
        System.out.println("immunizationsGiven :" + immunizationsGiven);

        // Create FHIR context and client
        FhirContext ctx = FhirContext.forR4();
        IGenericClient client = ctx.newRestfulGenericClient("http://localhost:9999/fhir");

        // Build the Parameters resource
        Parameters parameters = new Parameters();

        // Add assessmentDate parameter
        parameters.addParameter()
            .setName("assessmentDate")
            .setValue(new DateType(new Date(time)));

        // Create Patient resource
        Patient patient = new Patient();
        patient.setId("example");
        patient.setGender(Enumerations.AdministrativeGender.fromCode((String) person.attributes.get("gender")));
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
        for (ParametersParameterComponent parameter : out.getParameter()) {
            if (parameter.getName().equals("recommendation") && parameter.hasResource() && parameter.getResource() instanceof ImmunizationRecommendation) {
                immunizationRecommendation = (ImmunizationRecommendation) parameter.getResource();
                break;
            }
        }

        // Return the ImmunizationRecommendation resource
        return immunizationRecommendation;
    }
}