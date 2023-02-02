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
package org.apache.camel.quarkus.component.aws2.cw.it;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.cw.Cw2Constants;
import org.apache.camel.quarkus.test.support.aws2.BaseAws2Resource;

@Path("/aws2-cw")
@ApplicationScoped
public class Aws2CwResource extends BaseAws2Resource {

    @Inject
    ProducerTemplate producerTemplate;

    private volatile String endpointUri;

    public Aws2CwResource() {
        super("cw");
    }

    @Scheduled(every = "1s")
    void schedule() {
        if (endpointUri != null) {
            producerTemplate.requestBody(endpointUri, null, String.class);
        }
    }

    @Path("/send-metric/{namespace}/{metric-name}/{metric-unit}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(
            String value,
            @PathParam("namespace") String namespace,
            @PathParam("metric-name") String name,
            @PathParam("metric-unit") String unit) throws Exception {
        endpointUri = "aws2-cw://" + namespace + "?name=" + name + "&value=" + value + "&unit=" + unit;
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @Path("/send-metric-map/{namespace}")
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.TEXT_PLAIN)
    public Response postMap(
            @PathParam("namespace") String namespace,
            @HeaderParam("returnExceptionMessage") boolean returnExceptionMessage,
            @HeaderParam("customClientName") String customClientName,
            MultivaluedMap<String, String> formParams) throws Exception {

        String uri = "aws2-cw://" + namespace + "?useDefaultCredentialsProvider="
                + isUseDefaultCredentials();
        if (customClientName != null && !customClientName.isEmpty()) {
            uri = uri + "&autowiredEnabled=false&amazonCwClient=#" + customClientName;
        }

        Map<String, Object> typedHeaders = formParams.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey(),
                e -> {
                    final String val = e.getValue().get(0);
                    if (Cw2Constants.METRIC_TIMESTAMP.equals(e.getKey())) {
                        return Instant.ofEpochMilli(Long.parseLong(val));
                    } else if (Cw2Constants.METRIC_VALUE.equals(e.getKey())) {
                        return Long.parseLong(val);
                    } else if (Cw2Constants.METRIC_DIMENSIONS.equals(e.getKey())) {
                        String[] keyVal = val.split("=");
                        return Map.of(keyVal[0], keyVal[1]);
                    }
                    return val;
                }));
        try {
            producerTemplate.requestBodyAndHeaders(uri, null, typedHeaders, String.class);
        } catch (Exception e) {
            if (returnExceptionMessage && e instanceof CamelExecutionException && e.getCause() != null) {
                return Response
                        .ok(e.getCause().getMessage())
                        .build();
            }
            throw e;
        }

        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

}
