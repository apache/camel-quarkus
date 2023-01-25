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
package org.apache.camel.quarkus.component.smallrye.reactive.messaging.it;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.camel.ProducerTemplate;

@Path("/smallrye-reactive-messaging")
public class SmallRyeReactiveMessagingResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ResultsBean results;

    @Inject
    FilesMessageConsumer filesMessageConsumer;

    @Path("/post")
    @Consumes(MediaType.TEXT_PLAIN)
    @POST
    public Response post(String message) throws Exception {
        producerTemplate.asyncSendBody("direct:in", message);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @Path("/values")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String getValues() {
        List<String> values = results.getResults();
        Collections.sort(values);
        return String.join(",", values);
    }

    @Path("/file")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String getFiles() {
        List<String> bodies = filesMessageConsumer.getFileBodies();
        return String.join(",", bodies);
    }
}
