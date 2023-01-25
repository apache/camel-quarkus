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
package org.apache.camel.quarkus.core.languages.it;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;

@Path("/core-languages")
@ApplicationScoped
public class CoreLanguagesResource {

    @Inject
    ProducerTemplate template;

    @Inject
    @Named("tokenCounter")
    AtomicInteger tokenCounter;

    @Inject
    @Named("xmlTokenCounter")
    AtomicInteger xmlTokenCounter;

    @Path("/header/{route}/{key}/{value}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String header(String body, @PathParam("route") String route, @PathParam("key") String key,
            @PathParam("value") String value) {
        return template.requestBodyAndHeader("direct:" + route, body, key, value, String.class);
    }

    @Path("/route/{route}/{bodyType}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String resource(String body, @PathParam("route") String route, @PathParam("bodyType") String bodyType) {
        switch (bodyType) {
        case "String":
            return template.requestBody("direct:" + route, body, String.class);
        case "byte[]":
            return template.requestBody("direct:" + route, body.getBytes(StandardCharsets.UTF_8), String.class);
        case "ByteBuffer":
            return template.requestBody("direct:" + route, ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8)),
                    String.class);
        default:
            throw new IllegalStateException("Unexpected body type " + bodyType);
        }
    }

    @Path("/exchangeProperty/{route}/{key}/{value}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String exchangeProperty(String body, @PathParam("route") String route, @PathParam("key") String key,
            @PathParam("value") String value) {
        return template.request("direct:" + route, e -> e.getProperties().put(key, value)).getMessage().getBody(String.class);
    }

    @Path("/counter/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int tokenCounter(@PathParam("name") String name) {
        switch (name) {
        case "tokenCounter":
            return tokenCounter.get();
        case "xmlTokenCounter":
            return xmlTokenCounter.get();
        default:
            throw new IllegalStateException("Unexpected counter name: " + name);
        }
    }

}
