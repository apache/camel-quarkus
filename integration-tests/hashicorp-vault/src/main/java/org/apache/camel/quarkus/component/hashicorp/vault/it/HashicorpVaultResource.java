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
package org.apache.camel.quarkus.component.hashicorp.vault.it;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.util.ObjectHelper;

import static org.apache.camel.component.hashicorp.vault.HashicorpVaultConstants.SECRET_PATH;
import static org.apache.camel.component.hashicorp.vault.HashicorpVaultConstants.SECRET_VERSION;

@Path("/hashicorp-vault")
@ApplicationScoped
public class HashicorpVaultResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/secret")
    @POST
    public Response createSecret(
            @QueryParam("endpointUri") String endpointUri,
            @QueryParam("key") String key,
            @QueryParam("value") String value) throws Exception {

        producerTemplate.sendBody(endpointUri, Map.of(key, value));
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/secret/pojo")
    @POST
    public Response createSecretFromPojo(
            @QueryParam("secretA") String secretA,
            @QueryParam("secretB") String secretB,
            @QueryParam("secretC") String secretC) throws Exception {

        SecretPojo pojo = new SecretPojo();
        pojo.setSecretA(secretA);
        pojo.setSecretB(secretB);
        pojo.setSecretC(secretC);

        producerTemplate.sendBody("direct:createSecret", pojo);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @SuppressWarnings("unchecked")
    @Path("/secret")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSecret(
            @QueryParam("endpointUri") String endpointUri,
            @QueryParam("secretPath") String secretPath,
            @QueryParam("version") String version) {
        try {
            if (ObjectHelper.isEmpty(endpointUri)) {
                endpointUri = "direct:getSecret";
            }

            Map<String, Object> headers = new HashMap<>();
            headers.put(SECRET_PATH, secretPath);
            if (version != null) {
                headers.put(SECRET_VERSION, version);
            }

            Map<String, Map<String, String>> map = producerTemplate.requestBodyAndHeaders(endpointUri, null, headers,
                    Map.class);
            if (map != null && map.containsKey("data")) {
                Map<String, String> data = map.get("data");
                return Response.ok(data).build();
            }
            return Response.status(404).build();
        } catch (Exception e) {
            return Response.status(404).build();
        }
    }

    @Path("/secret/placeholder")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSecretFromPropertyPlaceholder(@QueryParam("secretPath") String secretPath,
            @QueryParam("version") String version) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(SECRET_PATH, secretPath);
        if (version != null) {
            headers.put(SECRET_VERSION, version);
        }
        return producerTemplate.requestBodyAndHeaders("direct:propertyPlaceholder", null, headers, String.class);
    }

    @Path("/secret")
    @DELETE
    public void deleteSecret() {
        producerTemplate.sendBody("direct:deleteSecret", null);
    }

    @SuppressWarnings("unchecked")
    @Path("/secret/list/all")
    @GET
    public List<String> listSecrets() {
        return producerTemplate.requestBody("direct:listSecrets", null, List.class);
    }
}
