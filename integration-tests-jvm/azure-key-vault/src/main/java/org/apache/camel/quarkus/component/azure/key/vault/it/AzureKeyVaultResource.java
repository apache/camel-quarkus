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
package org.apache.camel.quarkus.component.azure.key.vault.it;

import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.azure.key.vault.KeyVaultConstants;

@Path("/azure-key-vault")
@ApplicationScoped
public class AzureKeyVaultResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/secret/{secretName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createSecret(@PathParam("secretName") String secretName, String secret) {
        KeyVaultSecret result = producerTemplate.requestBodyAndHeader("direct:createSecret", secret,
                KeyVaultConstants.SECRET_NAME, secretName, KeyVaultSecret.class);
        return Response.ok(result.getName()).build();
    }

    @Path("/secret/{secretName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSecret(@PathParam("secretName") String secretName) {
        return producerTemplate.requestBodyAndHeader("direct:getSecret", null,
                KeyVaultConstants.SECRET_NAME, secretName, String.class);
    }

    @Path("/secret/{secretName}")
    @DELETE
    public Response deleteSecret(@PathParam("secretName") String secretName) {
        producerTemplate.requestBodyAndHeader("direct:deleteSecret", null,
                KeyVaultConstants.SECRET_NAME, secretName, Void.class);
        return Response.ok().build();
    }

    @Path("/secret/{secretName}/purge")
    @DELETE
    public Response purgeSecret(@PathParam("secretName") String secretName) {
        producerTemplate.requestBodyAndHeader("direct:purgeDeletedSecret", null,
                KeyVaultConstants.SECRET_NAME, secretName, Void.class);
        return Response.ok().build();
    }

    @Path("/secret/from/placeholder")
    @GET
    public String getSecretFromPropertyPlaceholder() {
        return producerTemplate.requestBody("direct:propertyPlaceholder", null, String.class);
    }
}
