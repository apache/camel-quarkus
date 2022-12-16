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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import io.quarkus.scheduler.Scheduled;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.cw.Cw2Constants;

@Path("/aws2-cw")
@ApplicationScoped
public class Aws2CwResource {

    @Inject
    ProducerTemplate producerTemplate;

    private volatile String endpointUri;

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
            @HeaderParam("customClientName") String customClientName,
            MultivaluedMap<String, String> formParams) throws Exception {

        String uri = "aws2-cw://" + namespace;
        uri = customClientName != null && !customClientName.isEmpty()
                ? uri + "?autowiredEnabled=false&amazonCwClient=#" + customClientName : uri;

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
            if (e instanceof CamelExecutionException && e.getCause() != null) {
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
