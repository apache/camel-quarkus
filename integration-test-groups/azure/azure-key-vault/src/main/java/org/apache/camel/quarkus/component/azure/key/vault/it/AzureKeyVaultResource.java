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

import java.util.concurrent.atomic.AtomicBoolean;

import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
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
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ResolveEndpointFailedException;
import org.apache.camel.component.azure.key.vault.KeyVaultConstants;
import org.apache.camel.impl.event.CamelContextReloadedEvent;
import org.jboss.logging.Logger;

@Path("/azure-key-vault")
@ApplicationScoped
public class AzureKeyVaultResource {
    private static final Logger LOG = Logger.getLogger(AzureKeyVaultResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext camelContext;

    static final AtomicBoolean contextReloaded = new AtomicBoolean(false);

    void onReload(@Observes CamelContextReloadedEvent event) {
        LOG.info("AzureKeyVaultResource onReload");
        contextReloaded.set(true);
    }

    @Path("/secret/routes/{command}")
    @POST
    public void startRoutes(@PathParam("command") String cmd) throws Exception {
        if ("start".equals(cmd)) {
            camelContext.getRouteController().startRoute("createSecret");
            camelContext.getRouteController().startRoute("getSecret");
            camelContext.getRouteController().startRoute("deleteSecret");
            camelContext.getRouteController().startRoute("purgeDeletedSecret");
        }
        if ("stop".equals(cmd)) {
            camelContext.getRouteController().stopRoute("createSecret");
            camelContext.getRouteController().stopRoute("getSecret");
            camelContext.getRouteController().stopRoute("deleteSecret");
            camelContext.getRouteController().stopRoute("purgeDeletedSecret");
        }
    }

    @Path("/secret/{identity}/{secretName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createSecret(@PathParam("secretName") String secretName, @PathParam("identity") boolean identity,
            String secret) {
        KeyVaultSecret result = producerTemplate.requestBodyAndHeader("direct:createSecret" + (identity ? "Identity" : ""),
                secret,
                KeyVaultConstants.SECRET_NAME, secretName, KeyVaultSecret.class);
        return Response.ok(result.getName()).build();
    }

    @Path("/secret/wrongClient/{secretName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createSecretWithWrongClient(@PathParam("secretName") String secretName,
            String secret) {
        try {
            KeyVaultSecret result = producerTemplate.requestBodyAndHeader("azure-key-vault://{{camel.vault.azure.vaultName}}" +
                    "?operation=createSecret",
                    secret,
                    KeyVaultConstants.SECRET_NAME, secretName, KeyVaultSecret.class);
            return Response.ok(result.getName()).build();
        } catch (ResolveEndpointFailedException e) {
            return Response.status(500).entity("ResolveEndpointFailedException").build();
        }
    }

    @Path("/secret/{identity}/{secretName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSecret(@PathParam("secretName") String secretName, @PathParam("identity") boolean identity) {
        return producerTemplate.requestBodyAndHeader("direct:getSecret" + (identity ? "Identity" : ""), null,
                KeyVaultConstants.SECRET_NAME, secretName, String.class);
    }

    @Path("/secret/{identity}/{secretName}")
    @DELETE
    public Response deleteSecret(@PathParam("secretName") String secretName, @PathParam("identity") boolean identity) {
        producerTemplate.requestBodyAndHeader("direct:deleteSecret" + (identity ? "Identity" : ""), null,
                KeyVaultConstants.SECRET_NAME, secretName, Void.class);
        return Response.ok().build();
    }

    @Path("/secret/{identity}/{secretName}/purge")
    @DELETE
    public Response purgeSecret(@PathParam("secretName") String secretName, @PathParam("identity") boolean identity) {
        producerTemplate.requestBodyAndHeader("direct:purgeDeletedSecret" + (identity ? "Identity" : ""), null,
                KeyVaultConstants.SECRET_NAME, secretName, Void.class);
        return Response.ok().build();
    }

    @Path("/secret/fromPlaceholder")
    @GET
    public String getSecretFromPropertyPlaceholder() {
        return producerTemplate.requestBody("direct:propertyPlaceholder", null, String.class);
    }

    @Path("/context/reload")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean contextReloadStatus() {
        return contextReloaded.get();
    }
}
