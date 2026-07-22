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
package org.apache.camel.quarkus.component.cyberark.vault.it;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.cyberark.vault.CyberArkVaultConstants;
import org.jboss.logging.Logger;

@Path("/cyberark-vault")
@ApplicationScoped
public class CyberarkVaultResource {

    private static final Logger LOG = Logger.getLogger(CyberarkVaultResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/createSecret/{authorized}/{policy}/{secret}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createSecret(String body, @PathParam("authorized") boolean authorized,
            @PathParam("policy") String policy, @PathParam("secret") String secret) {
        try {
            producerTemplate.requestBodyAndHeader(
                    "direct:createSecret" + (authorized ? "" : "Unauthorized"), body,
                    CyberArkVaultConstants.SECRET_ID, policy + "/" + secret, String.class);
        } catch (RuntimeException e) {
            return Response.serverError().entity(e.getCause().getCause().getMessage()).build();
        }
        return Response.ok().build();
    }

    @Path("/getSecret")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSecret() {
        return producerTemplate.requestBody("direct:getSecret", "", String.class);
    }

    @Path("/getSecretByHeader/{policy}/{secret}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSecretByHeader(@PathParam("policy") String policy, @PathParam("secret") String secret) {
        return producerTemplate.requestBodyAndHeader(
                "direct:getSecretByHeader", "", CyberArkVaultConstants.SECRET_ID, policy + "/" + secret, String.class);
    }

    @Path("/getSecretVersion/{version}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSecretVersion(@PathParam("version") int version) {
        return producerTemplate.requestBodyAndHeaders(
                "direct:getSecretVersion", "",
                Map.of(CyberArkVaultConstants.SECRET_VERSION, version),
                String.class);
    }

    @Path("/propertyPlaceholder")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String propertyPlaceholder() {
        return producerTemplate.requestBody("direct:propertyPlaceholder", "", String.class);
    }
}
