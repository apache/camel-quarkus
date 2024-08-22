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

@Path("/hashicorp-vault")
@ApplicationScoped
public class HashicorpVaultResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/secret")
    @POST
    public Response createSecret(@QueryParam("key") String key, @QueryParam("value") String value) throws Exception {
        producerTemplate.sendBody("direct:createSecret", Map.of(key, value));
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @SuppressWarnings("unchecked")
    @Path("/secret")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getSecret(@QueryParam("key") String key) {
        try {
            Map<String, Map<String, String>> map = producerTemplate.requestBody("direct:getSecret", null, Map.class);
            if (map.containsKey("data")) {
                Map<String, String> data = map.get("data");
                if (data.containsKey(key)) {
                    return Response.ok(data.get(key)).build();
                }
            }
            return Response.status(404).build();
        } catch (Exception e) {
            return Response.status(404).build();
        }
    }

    @Path("/secret/placeholder")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSecretFromPropertyPlaceholder() {
        return producerTemplate.requestBody("direct:propertyPlaceholder", null, String.class);
    }

    @Path("/secret")
    @DELETE
    public void deleteSecret() {
        producerTemplate.sendBody("direct:deleteSecret", null);
    }

    @SuppressWarnings("unchecked")
    @Path("/secret/list/all")
    @GET
    public String listSecrets() {
        List<String> secrets = producerTemplate.requestBody("direct:listSecrets", null, List.class);
        if (secrets.size() == 1) {
            return secrets.get(0);
        }
        throw new IllegalStateException("Expected a list containing 1 secret, but found " + secrets.size());
    }
}
