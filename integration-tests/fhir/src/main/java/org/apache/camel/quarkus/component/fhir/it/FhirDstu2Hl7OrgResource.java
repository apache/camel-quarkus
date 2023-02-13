/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.fhir.it;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PreferReturnEnum;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.fhir.FhirComponent;
import org.apache.camel.component.fhir.api.ExtraParameters;
import org.apache.camel.component.fhir.internal.FhirHelper;
import org.apache.camel.util.ObjectHelper;
import org.hl7.fhir.dstu2.model.Bundle;
import org.hl7.fhir.dstu2.model.Conformance;
import org.hl7.fhir.dstu2.model.HumanName;
import org.hl7.fhir.dstu2.model.IdType;
import org.hl7.fhir.dstu2.model.Meta;
import org.hl7.fhir.dstu2.model.Narrative;
import org.hl7.fhir.dstu2.model.OperationOutcome;
import org.hl7.fhir.dstu2.model.Parameters;
import org.hl7.fhir.dstu2.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;

import static org.apache.camel.quarkus.component.fhir.it.FhirConstants.PATIENT_ADDRESS;
import static org.apache.camel.quarkus.component.fhir.it.FhirConstants.PATIENT_FIRST_NAME;
import static org.apache.camel.quarkus.component.fhir.it.FhirConstants.PATIENT_LAST_NAME;

@Path("/dstu2hl7org")
@ApplicationScoped
public class FhirDstu2Hl7OrgResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Inject
    @Named("DSTU2_HL7ORG")
    Instance<FhirContext> fhirContextInstance;

    /////////////////////
    // Capabilities
    /////////////////////

    @Path("/capabilities")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String capabilities(@QueryParam("encodeAs") String encodeAs) {
        Map<String, Object> headers = new HashMap<>();
        if (encodeAs.equals("encodeJson") || encodeAs.equals("encodeXml")) {
            headers.put(encodeAs, Boolean.TRUE);
        }

        Conformance result = producerTemplate.requestBodyAndHeaders("direct:capabilities-dstu2-hl7org", Conformance.class,
                headers, Conformance.class);
        return result.getStatus().name();
    }

    /////////////////////
    // Create
    /////////////////////

    @Path("/createPatientAsStringResource")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject createPatientAsStringResource(
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("address") String address,
            @QueryParam("encodeAs") String encodeAs) {
        Patient patient = new Patient();
        patient.addAddress().addLine(address);
        patient.addName().addGiven(firstName).addFamily(lastName);

        String patientString = null;
        Map<String, Object> headers = new HashMap<>();
        headers.put(encodeAs, Boolean.TRUE);

        if (encodeAs.equals("encodeJson")) {
            patientString = fhirContextInstance.get().newJsonParser().encodeResourceToString(patient);
        } else {
            patientString = fhirContextInstance.get().newXmlParser().encodeResourceToString(patient);
        }

        MethodOutcome result = producerTemplate.requestBodyAndHeaders("direct:createResourceAsString-dstu2-hl7org",
                patientString,
                headers,
                MethodOutcome.class);

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("created", result.getCreated());
        builder.add("id", result.getId().getValue());
        builder.add("idPart", result.getId().getIdPart());
        builder.add("idUnqualifiedVersionless", result.getId().toUnqualifiedVersionless().getValue());
        builder.add("version", result.getId().getVersionIdPart());
        return builder.build();
    }

    @Path("/createPatient")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject createPatient(
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("address") String address) {
        Patient patient = new Patient();
        patient.addAddress().addLine(address);
        patient.addName().addGiven(firstName).addFamily(lastName);

        MethodOutcome result = producerTemplate.requestBody("direct:createResource-dstu2-hl7org", patient, MethodOutcome.class);

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("created", result.getCreated());
        builder.add("id", result.getId().getValue());
        builder.add("idPart", result.getId().getIdPart());
        builder.add("idUnqualifiedVersionless", result.getId().toUnqualifiedVersionless().getValue());
        builder.add("version", result.getId().getVersionIdPart());
        return builder.build();
    }

    /////////////////////
    // Dataformats
    /////////////////////

    @Path("/fhir2json")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response fhir2json(
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("address") String address) throws Exception {

        Patient patient = new Patient();
        patient.addAddress().addLine(address);
        patient.addName().addGiven(firstName).addFamily(lastName);

        String patientString = fhirContextInstance.get().newJsonParser().encodeResourceToString(patient);

        try (InputStream response = producerTemplate.requestBody("direct:json-to-dstu2-hl7org", patientString,
                InputStream.class)) {
            return Response
                    .created(new URI("https:camel.apache.org/"))
                    .entity(response)
                    .build();
        }
    }

    @Path("/fhir2xml")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response fhir2xml(
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("address") String address) throws Exception {

        Patient patient = new Patient();
        patient.addAddress().addLine(address);
        patient.addName().addGiven(firstName).addFamily(lastName);

        String patientString = fhirContextInstance.get().newXmlParser().encodeResourceToString(patient);

        try (InputStream response = producerTemplate.requestBody("direct:xml-to-dstu2-hl7org", patientString,
                InputStream.class)) {
            return Response
                    .created(new URI("https:camel.apache.org/"))
                    .entity(response)
                    .build();
        }
    }

    /////////////////////
    // Delete
    /////////////////////

    @Path("/deletePatient/byModel")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public String deletePatientByModel(@QueryParam("id") String id) {
        Patient patient = new Patient();
        patient.addAddress().addLine(PATIENT_ADDRESS);
        patient.addName().addGiven(PATIENT_FIRST_NAME).addFamily(PATIENT_LAST_NAME);
        patient.setId(id);

        MethodOutcome result = producerTemplate.requestBody("direct:delete-dstu2-hl7org", patient,
                MethodOutcome.class);
        return result.getId().getIdPart();
    }

    @Path("/deletePatient/byId")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public String deletePatientById(@QueryParam("id") String id) {
        MethodOutcome result = producerTemplate.requestBody("direct:deleteById-dstu2-hl7org",
                new IdType(id),
                MethodOutcome.class);
        return result.getId().getIdPart();
    }

    @Path("/deletePatient/byIdPart")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public String deletePatientByIdPart(@QueryParam("id") String id) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.type", "Patient");
        headers.put("CamelFhir.stringId", id);
        MethodOutcome result = producerTemplate.requestBodyAndHeaders("direct:deleteByStringId-dstu2-hl7org", null,
                headers,
                MethodOutcome.class);
        return result.getId().getIdPart();
    }

    @Path("/deletePatient/byUrl")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public String deletePatientByUrl(@QueryParam("cache") boolean noCache) {
        Map<String, Object> headers = new HashMap<>();
        if (noCache) {
            headers.put(ExtraParameters.CACHE_CONTROL_DIRECTIVE.getHeaderName(), new CacheControlDirective().setNoCache(true));
        }

        String body = String.format("Patient?given=%s&family=%s", PATIENT_FIRST_NAME, PATIENT_LAST_NAME);
        MethodOutcome result = producerTemplate.requestBodyAndHeaders("direct:deleteConditionalByUrl-dstu2-hl7org",
                body,
                headers,
                MethodOutcome.class);
        IBaseOperationOutcome operationOutcome = result.getOperationOutcome();
        return operationOutcome.getIdElement().getIdPart();
    }

    /////////////////////
    // History
    /////////////////////

    @Path("/history/onInstance")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int historyOnInstance(@QueryParam("id") String id) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.id", new IdType(id));
        headers.put("CamelFhir.returnType", Bundle.class);
        headers.put("CamelFhir.count", 1);

        Bundle result = producerTemplate.requestBodyAndHeaders("direct:historyOnInstance-dstu2-hl7org", null, headers,
                Bundle.class);
        return result.getEntry().size();
    }

    @Path("/history/onServer")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int historyOnServer() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.returnType", Bundle.class);
        headers.put("CamelFhir.count", 1);

        Bundle result = producerTemplate.requestBodyAndHeaders("direct:historyOnServer-dstu2-hl7org", null, headers,
                Bundle.class);
        return result.getEntry().size();
    }

    @Path("/history/onType")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int historyOnType() {
        final Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resourceType", Patient.class);
        headers.put("CamelFhir.returnType", Bundle.class);
        headers.put("CamelFhir.count", 1);

        Bundle result = producerTemplate.requestBodyAndHeaders("direct:historyOnType-dstu2-hl7org", null, headers,
                Bundle.class);
        return result.getEntry().size();
    }

    /////////////////////
    // Load page
    /////////////////////

    @Path("/load/page/byUrl")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int loadPageByUrl() {
        String url = "Patient?_count=2";
        Bundle bundle = getFhirClient()
                .search()
                .byUrl(url)
                .returnBundle(Bundle.class)
                .execute();

        String nextPageLink = bundle.getLink("next").getUrl();

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.url", nextPageLink);
        headers.put("CamelFhir.returnType", Bundle.class);

        Bundle result = producerTemplate.requestBodyAndHeaders("direct:loadPageByUrl-dstu2-hl7org", null, headers,
                Bundle.class);
        return result.getEntry().size();
    }

    @Path("/load/page/next")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int loadPageNext() {
        String url = "Patient?_count=2";
        Bundle bundle = getFhirClient()
                .search()
                .byUrl(url)
                .returnBundle(Bundle.class)
                .execute();

        Bundle result = producerTemplate.requestBody("direct:loadPageNext-dstu2-hl7org", bundle, Bundle.class);
        return result.getEntry().size();
    }

    @Path("/load/page/previous")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int loadPagePrevious(@QueryParam("encodeAsXml") boolean encodeAsXml) {
        String url = "Patient?_count=2";
        Bundle bundle = getFhirClient()
                .search()
                .byUrl(url)
                .returnBundle(Bundle.class)
                .execute();

        String nextPageLink = bundle.getLink("next").getUrl();
        bundle = getFhirClient()
                .loadPage()
                .byUrl(nextPageLink)
                .andReturnBundle(Bundle.class)
                .execute();

        Map<String, Object> headers = new HashMap<>();
        if (encodeAsXml) {
            headers.put(ExtraParameters.ENCODING_ENUM.getHeaderName(), EncodingEnum.XML);
        }

        Bundle result = producerTemplate.requestBodyAndHeaders("direct:loadPagePrevious-dstu2-hl7org", bundle, headers,
                Bundle.class);
        return result.getEntry().size();
    }

    /////////////////////
    // Meta
    /////////////////////

    @Path("/meta")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public int metaAdd(@QueryParam("id") String id) {
        IdType iIdType = new IdType(id);
        Meta inMeta = new Meta();
        inMeta.addTag().setSystem("urn:system1").setCode("urn:code1");

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.meta", inMeta);
        headers.put("CamelFhir.id", iIdType);

        IBaseMetaType result = producerTemplate.requestBodyAndHeaders("direct:metaAdd-dstu2-hl7org", null, headers,
                IBaseMetaType.class);
        return result.getTag().size();
    }

    @Path("/meta")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public int metaDelete(@QueryParam("id") String id) {
        IdType iIdType = new IdType(id);
        Meta inMeta = new Meta();
        inMeta.addTag().setSystem("urn:system1").setCode("urn:code1");

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.meta", inMeta);
        headers.put("CamelFhir.id", iIdType);

        IBaseMetaType result = producerTemplate.requestBodyAndHeaders("direct:metaDelete-dstu2-hl7org", null, headers,
                IBaseMetaType.class);
        return result.getTag().size();
    }

    @Path("/meta/getFromResource")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int metaGetFromResource(@QueryParam("id") String id) {
        IdType iIdType = new IdType(id);

        final Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.metaType", Meta.class);
        headers.put("CamelFhir.id", iIdType);

        IBaseMetaType result = producerTemplate.requestBodyAndHeaders("direct:metaGetFromResource-dstu2-hl7org", null, headers,
                IBaseMetaType.class);
        return result.getTag().size();
    }

    @Path("/meta/getFromServer")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int metaGetFromServer() {
        Meta result = producerTemplate.requestBody("direct:metaGetFromServer-dstu2-hl7org", Meta.class,
                Meta.class);
        return result.getTag().size();
    }

    @Path("/meta/getFromType")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int metaGetFromType(@QueryParam("preferResponseType") boolean preferResponseType) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.metaType", Meta.class);
        headers.put("CamelFhir.resourceType", "Patient");

        if (preferResponseType) {
            headers.put(ExtraParameters.PREFER_RESPONSE_TYPE.getHeaderName(), Patient.class);
        }

        IBaseMetaType result = producerTemplate.requestBodyAndHeaders("direct:metaGetFromType-dstu2-hl7org", null, headers,
                IBaseMetaType.class);
        return result.getTag().size();
    }

    /////////////////////
    // Operation
    /////////////////////

    @Path("/operation/onInstance")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String operationOnInstance(@QueryParam("id") String id) {
        IdType iIdType = new IdType(id);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.id", iIdType);
        headers.put("CamelFhir.name", "everything");
        headers.put("CamelFhir.parameters", null);
        headers.put("CamelFhir.outputParameterType", Parameters.class);
        headers.put("CamelFhir.useHttpGet", Boolean.FALSE);
        headers.put("CamelFhir.returnType", null);
        headers.put("CamelFhir.extraParameters", null);

        Parameters result = producerTemplate.requestBodyAndHeaders("direct:operationOnInstance-dstu2-hl7org", null, headers,
                Parameters.class);

        Parameters.ParametersParameterComponent parametersParameterComponent = result.getParameter().get(0);
        Bundle bundle = (Bundle) parametersParameterComponent.getResource();
        Bundle.BundleEntryComponent bundleEntryComponent = bundle.getEntry().get(0);
        return bundleEntryComponent.getResource().getIdElement().toUnqualifiedVersionless().getValue();
    }

    @Path("/operation/onInstanceVersion")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String operationOnInstanceVersion(@QueryParam("id") String id) {
        IdType iIdType = new IdType(id);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.id", iIdType);
        headers.put("CamelFhir.name", "everything");
        headers.put("CamelFhir.parameters", null);
        headers.put("CamelFhir.outputParameterType", Parameters.class);
        headers.put("CamelFhir.useHttpGet", Boolean.FALSE);
        headers.put("CamelFhir.returnType", null);
        headers.put("CamelFhir.extraParameters", null);

        Parameters result = producerTemplate.requestBodyAndHeaders("direct:operationOnInstanceVersion-dstu2-hl7org", null,
                headers,
                Parameters.class);

        Parameters.ParametersParameterComponent parametersParameterComponent = result.getParameter().get(0);
        Bundle bundle = (Bundle) parametersParameterComponent.getResource();
        Bundle.BundleEntryComponent bundleEntryComponent = bundle.getEntry().get(0);
        return bundleEntryComponent.getResource().getIdElement().toUnqualifiedVersionless().getValue();
    }

    @Path("/operation/onServer")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean operationOnServer() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.name", "$get-resource-counts");
        headers.put("CamelFhir.parameters", null);
        headers.put("CamelFhir.outputParameterType", Parameters.class);
        headers.put("CamelFhir.useHttpGet", Boolean.TRUE);
        headers.put("CamelFhir.returnType", null);
        headers.put("CamelFhir.extraParameters", null);

        Parameters result = producerTemplate.requestBodyAndHeaders("direct:operationOnServer-dstu2-hl7org", null, headers,
                Parameters.class);
        return result != null;
    }

    @Path("/operation/onType")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String operationOnType() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resourceType", Patient.class);
        headers.put("CamelFhir.name", "everything");
        headers.put("CamelFhir.parameters", null);
        headers.put("CamelFhir.outputParameterType", Parameters.class);
        headers.put("CamelFhir.useHttpGet", Boolean.FALSE);
        headers.put("CamelFhir.returnType", null);
        headers.put("CamelFhir.extraParameters", null);

        Parameters result = producerTemplate.requestBodyAndHeaders("direct:operationOnType-dstu2-hl7org", null, headers,
                Parameters.class);

        Parameters.ParametersParameterComponent parametersParameterComponent = result.getParameter().get(0);
        Bundle bundle = (Bundle) parametersParameterComponent.getResource();
        Bundle.BundleEntryComponent bundleEntryComponent = bundle.getEntry().get(0);
        return bundleEntryComponent.getResource().getIdElement().toUnqualifiedVersionless().getValue();
    }

    @Path("/operation/processMessage")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String operationProcessMessage() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.respondToUri", null);
        headers.put("CamelFhir.msgBundle", null);
        headers.put("CamelFhir.asynchronous", Boolean.FALSE);
        headers.put("CamelFhir.responseClass", null);
        headers.put("CamelFhir.extraParameters", null);

        IBaseBundle result = producerTemplate.requestBodyAndHeaders("direct:operationProcessMessage-dstu2-hl7org", null,
                headers,
                IBaseBundle.class);
        return result.getIdElement().getIdPart();
    }

    /////////////////////
    // Patch
    /////////////////////

    @Path("/patch/byId")
    @PATCH
    @Produces(MediaType.TEXT_PLAIN)
    public String patchById(@QueryParam("id") String id, String patch) {
        IdType iIdType = new IdType(id);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.patchBody", patch);
        headers.put("CamelFhir.id", iIdType);
        headers.put("CamelFhir.preferReturn", null);

        MethodOutcome result = producerTemplate.requestBodyAndHeaders("direct:patchById-dstu2-hl7org", null, headers,
                MethodOutcome.class);

        return getFhirClient()
                .read()
                .resource(Patient.class)
                .withId(result.getId())
                .preferResponseType(Patient.class)
                .execute()
                .getGender()
                .getDisplay();
    }

    @Path("/patch/byStringId")
    @PATCH
    @Produces(MediaType.TEXT_PLAIN)
    public String patchByStringId(
            @QueryParam("id") String id,
            @QueryParam("preferResponseTypes") boolean preferResponseTypes,
            String patch) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.patchBody", patch);
        headers.put("CamelFhir.stringId", id);
        headers.put("CamelFhir.preferReturn", null);

        if (preferResponseTypes) {
            List<Class<? extends IBaseResource>> preferredResponseTypes = new ArrayList<>();
            preferredResponseTypes.add(Patient.class);
            headers.put(ExtraParameters.PREFER_RESPONSE_TYPES.getHeaderName(), preferredResponseTypes);
        }

        MethodOutcome result = producerTemplate.requestBodyAndHeaders("direct:patchBySid-dstu2-hl7org", null, headers,
                MethodOutcome.class);

        return getFhirClient()
                .read()
                .resource(Patient.class)
                .withId(result.getId())
                .preferResponseType(Patient.class)
                .execute()
                .getGender()
                .getDisplay();
    }

    @Path("/patch/byUrl")
    @PATCH
    @Produces(MediaType.TEXT_PLAIN)
    public String patchByUrl(@QueryParam("id") String id, String patch) throws UnsupportedEncodingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.patchBody", patch);
        headers.put("CamelFhir.url", "Patient?" + Patient.SP_RES_ID + "=" + id);
        headers.put("CamelFhir.preferReturn", null);

        MethodOutcome result = producerTemplate.requestBodyAndHeaders("direct:patchByUrl-dstu2-hl7org", null, headers,
                MethodOutcome.class);

        return getFhirClient()
                .read()
                .resource(Patient.class)
                .withId(result.getId())
                .preferResponseType(Patient.class)
                .execute()
                .getGender()
                .getDisplay();
    }

    /////////////////////
    // Read
    /////////////////////

    @Path("/readPatient/byId")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readPatientById(@QueryParam("id") String id) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resource", Patient.class);
        headers.put("CamelFhir.id", new IdType(id));

        try {
            Patient result = producerTemplate.requestBodyAndHeaders("direct:readById-dstu2-hl7org", null, headers,
                    Patient.class);
            return Response.ok().entity(patientToJsonObject(result)).build();
        } catch (CamelExecutionException e) {
            Throwable cause = e.getExchange().getException().getCause();
            if (cause instanceof ResourceGoneException) {
                return Response.status(404).build();
            }
        }

        return Response.noContent().build();
    }

    @Path("/readPatient/byLongId")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readPatientByLongId(@QueryParam("id") String id) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resource", Patient.class);
        headers.put("CamelFhir.longId", Long.valueOf(id));
        try {
            Patient result = producerTemplate.requestBodyAndHeaders("direct:readByLongId-dstu2-hl7org", null, headers,
                    Patient.class);
            return Response.ok().entity(patientToJsonObject(result)).build();
        } catch (CamelExecutionException e) {
            Throwable cause = e.getExchange().getException().getCause();
            if (cause instanceof ResourceGoneException) {
                return Response.status(404).build();
            }
        }

        return Response.noContent().build();
    }

    @Path("/readPatient/byStringId")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readPatientByStringId(@QueryParam("id") String id) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resource", Patient.class);
        headers.put("CamelFhir.stringId", id);
        try {
            Patient result = producerTemplate.requestBodyAndHeaders("direct:readByStringId-dstu2-hl7org", null, headers,
                    Patient.class);
            return Response.ok().entity(patientToJsonObject(result)).build();
        } catch (CamelExecutionException e) {
            Throwable cause = e.getExchange().getException().getCause();
            if (cause instanceof ResourceGoneException) {
                return Response.status(404).build();
            }
        }

        return Response.noContent().build();
    }

    @Path("/readPatient/byIdAndStringResource")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readPatientByIdAndStringResource(@QueryParam("id") String id) {
        IdType iIdType = new IdType(id);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resourceClass", "Patient");
        headers.put("CamelFhir.id", iIdType);
        try {
            Patient result = producerTemplate.requestBodyAndHeaders("direct:readByIdAndStringResource-dstu2-hl7org", null,
                    headers,
                    Patient.class);
            return Response.ok().entity(patientToJsonObject(result)).build();
        } catch (CamelExecutionException e) {
            Throwable cause = e.getExchange().getException().getCause();
            if (cause instanceof ResourceGoneException) {
                return Response.status(404).build();
            }
        }

        return Response.noContent().build();
    }

    @Path("/readPatient/byLongIdAndStringResource")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readPatientByLongIdAndStringResource(@QueryParam("id") String id) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resourceClass", "Patient");
        headers.put("CamelFhir.longId", Long.valueOf(id));
        try {
            Patient result = producerTemplate.requestBodyAndHeaders("direct:readByLongIdAndStringResource-dstu2-hl7org", null,
                    headers,
                    Patient.class);
            return Response.ok().entity(patientToJsonObject(result)).build();
        } catch (CamelExecutionException e) {
            Throwable cause = e.getExchange().getException().getCause();
            if (cause instanceof ResourceGoneException) {
                return Response.status(404).build();
            }
        }

        return Response.noContent().build();
    }

    @Path("/readPatient/byStringIdAndStringResource")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readPatientByStringIdAndStringResource(@QueryParam("id") String id) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resourceClass", "Patient");
        headers.put("CamelFhir.stringId", id);
        try {
            Patient result = producerTemplate.requestBodyAndHeaders("direct:readByStringIdAndStringResource-dstu2-hl7org", null,
                    headers,
                    Patient.class);
            return Response.ok().entity(patientToJsonObject(result)).build();
        } catch (CamelExecutionException e) {
            Throwable cause = e.getExchange().getException().getCause();
            if (cause instanceof ResourceGoneException) {
                return Response.status(404).build();
            }
        }

        return Response.noContent().build();
    }

    @Path("/readPatient/byStringIdAndVersion")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readPatientByStringIdAndVersion(@QueryParam("id") String id, @QueryParam("version") String version) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resource", Patient.class);
        headers.put("CamelFhir.stringId", id);
        headers.put("CamelFhir.version", version);
        try {
            Patient result = producerTemplate.requestBodyAndHeaders("direct:readByStringIdAndVersion-dstu2-hl7org", null,
                    headers,
                    Patient.class);
            return Response.ok().entity(patientToJsonObject(result)).build();
        } catch (CamelExecutionException e) {
            Throwable cause = e.getExchange().getException().getCause();
            if (cause instanceof ResourceGoneException) {
                return Response.status(404).build();
            }
        }

        return Response.noContent().build();
    }

    @Path("/readPatient/byStringIdAndVersionWithResourceClass")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readPatientByStringIdAndVersionWithResourceClass(@QueryParam("id") String id,
            @QueryParam("version") String version) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resourceClass", "Patient");
        headers.put("CamelFhir.stringId", id);
        headers.put("CamelFhir.version", version);
        try {
            Patient result = producerTemplate.requestBodyAndHeaders(
                    "direct:readByStringIdAndVersionAndStringResource-dstu2-hl7org",
                    null,
                    headers, Patient.class);
            return Response.ok().entity(patientToJsonObject(result)).build();
        } catch (CamelExecutionException e) {
            Throwable cause = e.getExchange().getException().getCause();
            if (cause instanceof ResourceGoneException) {
                return Response.status(404).build();
            }
        }

        return Response.noContent().build();
    }

    @Path("/readPatient/byIUrl")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readPatientByIUrl(@QueryParam("id") String id) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resource", Patient.class);
        headers.put("CamelFhir.iUrl", new IdType(id));
        try {
            Patient result = producerTemplate.requestBodyAndHeaders("direct:readByIUrl-dstu2-hl7org", null, headers,
                    Patient.class);
            return Response.ok().entity(patientToJsonObject(result)).build();
        } catch (CamelExecutionException e) {
            Throwable cause = e.getExchange().getException().getCause();
            if (cause instanceof ResourceGoneException) {
                return Response.status(404).build();
            }
        }

        return Response.noContent().build();
    }

    @Path("/readPatient/byUrl")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readPatientByUrl(@QueryParam("id") String id) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resource", Patient.class);
        headers.put("CamelFhir.url", id);
        try {
            Patient result = producerTemplate.requestBodyAndHeaders("direct:readByUrl-dstu2-hl7org", null, headers,
                    Patient.class);
            return Response.ok().entity(patientToJsonObject(result)).build();
        } catch (CamelExecutionException e) {
            Throwable cause = e.getExchange().getException().getCause();
            if (cause instanceof ResourceGoneException) {
                return Response.status(404).build();
            }
        }

        return Response.noContent().build();
    }

    @Path("/readPatient/byStringUrlAndStringResource")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readPatientByStringUrlAndStringResource(@QueryParam("id") String id) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resourceClass", "Patient");
        headers.put("CamelFhir.iUrl", new IdType(id));
        try {
            Patient result = producerTemplate.requestBodyAndHeaders("direct:readByStringUrlAndStringResource-dstu2-hl7org",
                    null,
                    headers,
                    Patient.class);
            return Response.ok().entity(patientToJsonObject(result)).build();
        } catch (CamelExecutionException e) {
            Throwable cause = e.getExchange().getException().getCause();
            if (cause instanceof ResourceGoneException) {
                return Response.status(404).build();
            }
        }

        return Response.noContent().build();
    }

    @Path("/readPatient/byUrlAndStringResource")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readByUrlAndStringResource(@QueryParam("id") String id, @QueryParam("prettyPrint") boolean prettyPrint) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resourceClass", "Patient");
        headers.put("CamelFhir.url", id);

        if (prettyPrint) {
            headers.put(ExtraParameters.PRETTY_PRINT.getHeaderName(), Boolean.TRUE);
        }

        try {
            Patient result = producerTemplate.requestBodyAndHeaders("direct:readByUrlAndStringResource-dstu2-hl7org", null,
                    headers,
                    Patient.class);
            return Response.ok().entity(patientToJsonObject(result)).build();
        } catch (CamelExecutionException e) {
            Throwable cause = e.getExchange().getException().getCause();
            if (cause instanceof ResourceGoneException) {
                return Response.status(404).build();
            }
        }

        return Response.noContent().build();
    }

    /////////////////////
    // Search
    /////////////////////

    @Path("/search/byUrl")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchByUrl(@QueryParam("id") String id) {
        String url = "Patient?" + Patient.SP_RES_ID + "=" + id + "&_format=json";
        Bundle result = producerTemplate.requestBody("direct:searchByUrl-dstu2-hl7org", url, Bundle.class);

        List<Bundle.BundleEntryComponent> entry = result.getEntry();
        if (ObjectHelper.isNotEmpty(entry)) {
            Patient patient = (Patient) entry.get(0).getResource();
            return Response.ok().entity(patientToJsonObject(patient)).build();
        }
        return Response.status(404).build();
    }

    /////////////////////
    // Transaction
    /////////////////////

    @Path("/transaction/withBundle")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String transactionWithBundle() {
        Bundle result = producerTemplate.requestBody("direct:transactionWithBundle-dstu2-hl7org", createTransactionBundle(),
                Bundle.class);
        return result.getEntry().get(0).getResponse().getStatus();
    }

    @Path("/transaction/withStringBundle")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String transactionWithStringBundle() {
        Bundle transactionBundle = createTransactionBundle();
        String stringBundle = fhirContextInstance.get().newJsonParser().encodeResourceToString(transactionBundle);
        return producerTemplate.requestBody("direct:transactionWithStringBundle-dstu2-hl7org", stringBundle, String.class);
    }

    @Path("/transaction/withResources")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @SuppressWarnings("unchecked")
    public int transactionWithResources(@QueryParam("summaryEnum") boolean summaryEnum) {
        Patient oscar = new Patient()
                .addName(new HumanName().addGiven("Oscar").addFamily("Peterson"));
        Patient bobbyHebb = new Patient()
                .addName(new HumanName().addGiven("Bobby").addFamily("Hebb"));
        List<IBaseResource> patients = new ArrayList<>(2);
        patients.add(oscar);
        patients.add(bobbyHebb);

        Map<String, Object> headers = new HashMap<>();
        if (summaryEnum) {
            headers.put(ExtraParameters.SUMMARY_ENUM.getHeaderName(), SummaryEnum.DATA);
        }

        List<IBaseResource> result = producerTemplate.requestBodyAndHeaders("direct:transactionWithResources-dstu2-hl7org",
                patients,
                headers, List.class);
        return result.size();
    }

    /////////////////////
    // Update
    /////////////////////

    @Path("/update/resource")
    @POST
    public void updateResource(@QueryParam("id") String id) throws ParseException {
        Patient patient = getFhirClient().read()
                .resource(Patient.class)
                .withId(id)
                .preferResponseType(Patient.class)
                .execute();

        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("1998-04-29");
        patient.setBirthDate(date);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resource", patient);
        headers.put("CamelFhir.id", patient.getIdElement());
        headers.put("CamelFhir.preferReturn", PreferReturnEnum.REPRESENTATION);

        producerTemplate.sendBodyAndHeaders("direct:updateResource-dstu2-hl7org", null, headers);
    }

    @Path("/update/resource/withoutId")
    @POST
    public void updateResourceWithoutId(@QueryParam("id") String id) throws ParseException {
        Patient patient = getFhirClient().read()
                .resource(Patient.class)
                .withId(id)
                .preferResponseType(Patient.class)
                .execute();

        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("1998-04-29");
        patient.setBirthDate(date);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resource", patient);
        headers.put("CamelFhir.preferReturn", PreferReturnEnum.REPRESENTATION);

        producerTemplate.sendBodyAndHeaders("direct:updateResource-dstu2-hl7org", null, headers);
    }

    @Path("/update/resource/withStringId")
    @POST
    public void updateResourceWithStringId(@QueryParam("id") String id) throws ParseException {
        Patient patient = getFhirClient().read()
                .resource(Patient.class)
                .withId(id)
                .preferResponseType(Patient.class)
                .execute();

        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("1998-04-29");
        patient.setBirthDate(date);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resource", patient);
        headers.put("CamelFhir.stringId", patient.getIdElement().getIdPart());
        headers.put("CamelFhir.preferReturn", PreferReturnEnum.REPRESENTATION);

        producerTemplate.sendBodyAndHeaders("direct:updateResourceWithStringId-dstu2-hl7org", null, headers);
    }

    @Path("/update/resource/asString")
    @POST
    public void updateResourceAsString(@QueryParam("id") String id) throws ParseException {
        Patient patient = getFhirClient().read()
                .resource(Patient.class)
                .withId(id)
                .preferResponseType(Patient.class)
                .execute();

        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("1998-04-29");
        patient.setBirthDate(date);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resourceAsString", fhirContextInstance.get().newJsonParser().encodeResourceToString(patient));
        headers.put("CamelFhir.id", patient.getIdElement());
        headers.put("CamelFhir.preferReturn", PreferReturnEnum.REPRESENTATION);

        producerTemplate.sendBodyAndHeaders("direct:updateResourceAsString-dstu2-hl7org", null, headers);
    }

    @Path("/update/resource/asStringWithStringId")
    @POST
    public void updateResourceAsStringWithStringId(@QueryParam("id") String id) throws ParseException {
        Patient patient = getFhirClient().read()
                .resource(Patient.class)
                .withId(id)
                .preferResponseType(Patient.class)
                .execute();

        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("1998-04-29");
        patient.setBirthDate(date);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resourceAsString", fhirContextInstance.get().newJsonParser().encodeResourceToString(patient));
        headers.put("CamelFhir.stringId", patient.getIdElement().getIdPart());
        headers.put("CamelFhir.preferReturn", PreferReturnEnum.REPRESENTATION);

        producerTemplate.sendBodyAndHeaders("direct:updateResourceAsStringWithStringId-dstu2-hl7org", null, headers);
    }

    @Path("/update/resource/bySearchUrl")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String updateResourceBySearchUrl(@QueryParam("id") String id) throws Exception {
        Patient patient = getFhirClient().read()
                .resource(Patient.class)
                .withId(id)
                .preferResponseType(Patient.class)
                .execute();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = dateFormat.parse("1998-04-29");
        patient.setBirthDate(date);

        String url = "Patient?" + Patient.SP_IDENTIFIER + '=' + URLEncoder.encode(patient.getIdElement().getIdPart(), "UTF-8");

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resource", patient);
        headers.put("CamelFhir.url", url);
        headers.put("CamelFhir.preferReturn", PreferReturnEnum.REPRESENTATION);

        MethodOutcome result = producerTemplate.requestBodyAndHeaders("direct:updateResourceBySearchUrl-dstu2-hl7org", null,
                headers,
                MethodOutcome.class);
        Patient updated = (Patient) result.getResource();
        return dateFormat.format(updated.getBirthDate());
    }

    @Path("/update/resource/bySearchUrlAndResourceAsString")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String updateResourceByUrlAndResourceAsString(@QueryParam("id") String id) throws Exception {
        Patient patient = getFhirClient().read()
                .resource(Patient.class)
                .withId(id)
                .preferResponseType(Patient.class)
                .execute();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = dateFormat.parse("1998-04-29");
        patient.setBirthDate(date);

        String url = "Patient?" + Patient.SP_IDENTIFIER + '=' + URLEncoder.encode(patient.getId().toString(), "UTF-8");

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelFhir.resourceAsString", fhirContextInstance.get().newJsonParser().encodeResourceToString(patient));
        headers.put("CamelFhir.url", url);
        headers.put("CamelFhir.preferReturn", PreferReturnEnum.REPRESENTATION);

        MethodOutcome result = producerTemplate.requestBodyAndHeaders(
                "direct:updateResourceBySearchUrlAndResourceAsString-dstu2-hl7org",
                null, headers, MethodOutcome.class);

        Patient updated = (Patient) result.getResource();
        return dateFormat.format(updated.getBirthDate());
    }

    /////////////////////
    // Validate
    /////////////////////

    @Path("/validate/resource")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String validateResource() {
        Patient patient = new Patient()
                .addName(new HumanName()
                        .addGiven(PATIENT_FIRST_NAME)
                        .addFamily(PATIENT_LAST_NAME));

        patient.getText().setStatus(Narrative.NarrativeStatus.GENERATED);
        patient.getText().setDivAsString("<div>This is the narrative text</div>");

        MethodOutcome result = producerTemplate.requestBody("direct:validateResource-dstu2-hl7org", patient,
                MethodOutcome.class);

        OperationOutcome operationOutcome = (OperationOutcome) result.getOperationOutcome();
        return operationOutcome.getIssue().get(0).getDiagnostics();
    }

    @Path("/validate/resourceAsString")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String validateResourceAsString() {
        Patient patient = new Patient()
                .addName(new HumanName()
                        .addGiven(PATIENT_FIRST_NAME)
                        .addFamily(PATIENT_LAST_NAME));

        patient.getText().setStatus(Narrative.NarrativeStatus.GENERATED);
        patient.getText().setDivAsString("<div>This is the narrative text</div>");

        String body = this.fhirContextInstance.get().newXmlParser().encodeResourceToString(patient);
        MethodOutcome result = producerTemplate.requestBody("direct:validateResourceAsString-dstu2-hl7org", body,
                MethodOutcome.class);

        OperationOutcome operationOutcome = (OperationOutcome) result.getOperationOutcome();
        return operationOutcome.getIssue().get(0).getDiagnostics();
    }

    private JsonObject patientToJsonObject(Patient patient) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("address", patient.getAddress().get(0).getLine().get(0).getValueAsString());
        builder.add("firstName", patient.getName().get(0).getGiven().get(0).getValueAsString());
        builder.add("lastName", patient.getName().get(0).getFamily().get(0).getValueAsString());

        Date birthDate = patient.getBirthDate();
        if (birthDate != null) {
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(birthDate);
            builder.add("birthDate", formattedDate);
        }
        return builder.build();
    }

    private Bundle createTransactionBundle() {
        Bundle input = new Bundle();
        input.setType(Bundle.BundleType.TRANSACTION);
        input.addEntry()
                .setResource(new Patient()
                        .addName(new HumanName().addGiven("Art").addFamily("Tatum")))
                .getRequest()
                .setMethod(Bundle.HTTPVerb.POST);
        return input;
    }

    private IGenericClient getFhirClient() {
        FhirComponent component = context.getComponent("fhir-dstu2-hl7org", FhirComponent.class);
        return FhirHelper.createClient(component.getConfiguration(), context);
    }
}
