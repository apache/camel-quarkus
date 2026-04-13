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
package org.apache.camel.quarkus.component.ibm.watson.language.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.ibm.watson.language.WatsonLanguageConstants;

@Path("/ibm-watson-language")
@ApplicationScoped
public class IbmWatsonLanguageResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/analyze/text/{operation}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response analyzeText(@PathParam("operation") String operation, String text) {
        try {
            String endpoint = "direct:analyze-text-" + operation;
            Object result = producerTemplate.requestBodyAndHeader(endpoint, text,
                    WatsonLanguageConstants.TEXT, text);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(500).entity(e.getMessage()).build();
        }
    }

    @Path("/analyze/url")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response analyzeUrl(String url) {
        try {
            Object result = producerTemplate.requestBodyAndHeader("direct:analyze-url", url,
                    WatsonLanguageConstants.URL, url);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(500).entity(e.getMessage()).build();
        }
    }
}
