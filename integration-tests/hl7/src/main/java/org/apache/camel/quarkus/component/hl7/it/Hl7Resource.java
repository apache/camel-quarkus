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
package org.apache.camel.quarkus.component.hl7.it;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import ca.uhn.hl7v2.model.AbstractMessage;
import ca.uhn.hl7v2.model.v22.datatype.AD;
import ca.uhn.hl7v2.model.v22.datatype.CK;
import ca.uhn.hl7v2.model.v22.datatype.PN;
import ca.uhn.hl7v2.model.v22.datatype.ST;
import ca.uhn.hl7v2.model.v22.datatype.TN;
import ca.uhn.hl7v2.model.v22.message.ADT_A01;
import ca.uhn.hl7v2.model.v22.segment.PID;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;

@Path("/hl7")
@ApplicationScoped
public class Hl7Resource {

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/mllp")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject mllp(String message) throws Exception {
        MockEndpoint endpoint = context.getEndpoint("mock:result", MockEndpoint.class);
        endpoint.expectedMessageCount(1);

        producerTemplate.sendBody(
                "netty:tcp://localhost:{{camel.hl7.test-tcp-port}}?sync=true&encoders=#hl7encoder&decoders=#hl7decoder",
                message);

        endpoint.assertIsSatisfied(5000L);
        Exchange exchange = endpoint.getExchanges().get(0);
        ADT_A01 result = exchange.getMessage().getBody(ADT_A01.class);

        return adtToJsonObject(result);
    }

    @Path("/marshalUnmarshal")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response marshalUnmarshal(@QueryParam("charset") String charset, String message) {
        Response.ResponseBuilder builder = Response.ok();
        Map<String, Object> headers = new HashMap<>();
        if (charset != null) {
            headers.put(Exchange.CHARSET_NAME, charset);
            builder.header("Content-Type", MediaType.TEXT_PLAIN + ";" + charset);
        }

        String result = producerTemplate.requestBodyAndHeaders("direct:marshalUnmarshal", message, headers, String.class);

        return builder.entity(result).build();
    }

    @Path("/validate")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response validate(String message) {
        Exchange exchange = producerTemplate.request("direct:validate", e -> e.getMessage().setBody(message));
        if (exchange.isFailed()) {
            Exception exception = exchange.getException();
            return Response.serverError().entity(exception.getMessage()).build();
        }
        return Response.ok().build();
    }

    @Path("/validate/custom")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response validateCustom(String message) {
        Exchange exchange = producerTemplate.request("direct:validateCustom", e -> e.getMessage().setBody(message));
        if (exchange.isFailed()) {
            Exception exception = exchange.getException();
            return Response.serverError().entity(exception.getMessage()).build();
        }
        return Response.ok().build();
    }

    @Path("/hl7terser")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String hl7terser(String message) {
        Exchange exchange = producerTemplate.request("direct:hl7terser", e -> e.getMessage().setBody(message));
        return exchange.getMessage().getHeader("PATIENT_ID", String.class);
    }

    @Path("/hl7terser/bean")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String hl7terserBean(String message) {
        return producerTemplate.requestBody("direct:hl7terserBean", message, String.class);
    }

    @Path("/xml")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject hl7Xml(String messageXml) {
        ADT_A01 result = producerTemplate.requestBody("direct:unmarshalXml", messageXml, ADT_A01.class);
        return adtToJsonObject(result);
    }

    @Path("/ack")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String validateWithAck(String message) {
        return producerTemplate.requestBody("direct:ack", message, String.class);
    }

    @Path("/convert/{version}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String convertStringToAdt(@PathParam("version") String version, String message) throws ClassNotFoundException {
        String adtClassName = "ca.uhn.hl7v2.model." + version.toLowerCase() + ".message.ADT_A01";
        Class<?> adtClass = Class.forName(adtClassName);
        AbstractMessage result = (AbstractMessage) context.getTypeConverter().convertTo(adtClass, message);
        return result.getMessage().getVersion();
    }

    private JsonObject adtToJsonObject(ADT_A01 result) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

        PID pid = result.getPID();
        PN patientName = pid.getPatientName();
        objectBuilder.add("first_name", patientName.getGivenName().getValue());
        objectBuilder.add("last_name", patientName.getFamilyName().getValue());

        ST birthPlace = pid.getBirthPlace();
        objectBuilder.add("birth_place", birthPlace.getValue());

        CK patientAccountNumber = pid.getPatientAccountNumber();
        objectBuilder.add("account_number", patientAccountNumber.getIDNumber().getValue());

        AD patientAddress = pid.getPatientAddress(0);
        objectBuilder.add("street", patientAddress.getAd1_StreetAddress().getValue());
        objectBuilder.add("city", patientAddress.getAd3_City().getValue());
        objectBuilder.add("zip", patientAddress.getZipOrPostalCode().getValue());

        TN phoneNumber = pid.getPhoneNumberHome(0);
        objectBuilder.add("phone", phoneNumber.getValue());

        return objectBuilder.build();
    }
}
