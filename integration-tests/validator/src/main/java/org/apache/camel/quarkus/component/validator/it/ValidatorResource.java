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
package org.apache.camel.quarkus.component.validator.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.processor.validation.NoXmlHeaderValidationException;

@Path("/validator")
@ApplicationScoped
public class ValidatorResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/validate/{directName}")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public Response processOrderInXml(String statement,
            @QueryParam("sourceHeader") String headerSourceContent,
            @PathParam("directName") String directName) {

        //if statement == "", use null instead
        String body = statement != null && !statement.isEmpty() ? statement : null;

        try {
            if (headerSourceContent != null) {
                return Response.ok().entity(producerTemplate.requestBodyAndHeader("direct:" + directName, null, "source",
                        headerSourceContent, String.class)).build();
            }

            return Response.ok().entity(producerTemplate.requestBody("direct:" + directName, body, String.class)).build();

        } catch (Exception e) {
            if (e.getCause() instanceof NoXmlHeaderValidationException) {
                return Response.serverError().entity(e.getCause().getMessage()).build();
            }

            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
