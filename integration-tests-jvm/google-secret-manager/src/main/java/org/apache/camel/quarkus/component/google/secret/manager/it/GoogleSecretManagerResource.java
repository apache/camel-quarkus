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
package org.apache.camel.quarkus.component.google.secret.manager.it;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.google.secret.manager.GoogleSecretManagerOperations;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/google-secret-manager")
@ApplicationScoped
public class GoogleSecretManagerResource {

    private static final Logger LOG = Logger.getLogger(GoogleSecretManagerResource.class);

    @ConfigProperty(name = "cq.google-secrets-manager.path-to-service-account-key")
    String accountKey;

    @ConfigProperty(name = "cq.google-secrets-manager.project-name")
    String projectName;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/list/{secretName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public List<String> getSecret(@PathParam("secretName") String secretName) {
        SecretManagerServiceClient.ListSecretsPagedResponse secrets = producerTemplate.requestBody("direct:listSecrets", "",
                SecretManagerServiceClient.ListSecretsPagedResponse.class);
        LinkedList<String> result = new LinkedList<>();
        SecretManagerServiceClient.ListSecretsPage page = secrets.getPage();
        while (page != null) {
            page.getValues().iterator().forEachRemaining(s -> result.add(s.getName()));
            page = page.getNextPage();
        }

        return result;
    }

    @Path("/getGcpSecret")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String loadGcpPassword() {
        return producerTemplate.requestBody("direct:loadGcpPassword", "", String.class);
    }

    @Path("/operation/{operation}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response operation(@PathParam("operation") String operation, @QueryParam("body") String body,
            Map<String, Object> headers) {

        Exchange ex = producerTemplate.send(String.format("google-secret-manager://%s" +
                "?serviceAccountKey=file:%s" +
                "&operation=%s", projectName, accountKey, operation),
                e -> {
                    e.getIn().setHeaders(headers == null ? Collections.emptyMap() : headers);
                    e.getIn().setBody(body == null ? "" : body);
                });

        Object result = null;
        switch (GoogleSecretManagerOperations.valueOf(operation)) {
        case listSecrets:

            LinkedList<String> listedSecrets = new LinkedList<>();
            SecretManagerServiceClient.ListSecretsPagedResponse response = ex.getIn()
                    .getBody(SecretManagerServiceClient.ListSecretsPagedResponse.class);
            SecretManagerServiceClient.ListSecretsPage page = response.getPage();
            while (page != null) {
                page.getValues().iterator().forEachRemaining(s -> listedSecrets.add(s.getName()));
                page = page.getNextPage();
            }

            result = listedSecrets;
            break;
        case createSecret:
            SecretVersion createdSecret = ex.getIn().getBody(SecretVersion.class);
            result = createdSecret.getName();
            break;
        case deleteSecret:
            result = true;
            break;
        case getSecretVersion:
            result = ex.getIn().getBody(String.class);
            break;
        default:
            return Response.status(500).build();
        }

        return Response.ok(result).build();
    }

}
