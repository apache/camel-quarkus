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
import java.net.URI;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import ca.uhn.fhir.rest.api.MethodOutcome;
import org.apache.camel.ProducerTemplate;

@Path("/dstu3")
@ApplicationScoped
public class FhirDstu3Resource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/fhir2json")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response fhir2json(String patient) throws Exception {
        try (InputStream response = producerTemplate.requestBody("direct:json-to-dstu3", patient, InputStream.class)) {
            return Response
                    .created(new URI("https://camel.apache.org/"))
                    .entity(response)
                    .build();
        }
    }

    @Path("/fhir2xml")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response fhir2xml(String patient) throws Exception {
        try (InputStream response = producerTemplate.requestBody("direct:xml-to-dstu3", patient, InputStream.class)) {
            return Response
                    .created(new URI("https://camel.apache.org/"))
                    .entity(response)
                    .build();
        }
    }

    @Path("/createPatient")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createPatient(String patient) throws Exception {
        MethodOutcome result = producerTemplate.requestBody("direct:create-dstu3", patient, MethodOutcome.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(result.getId().getIdPart())
                .build();
    }
}
