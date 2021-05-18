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
package org.apache.camel.quarkus.language.simple;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;

@Path("/simple")
@ApplicationScoped
public class SimpleResource {

    @Inject
    ProducerTemplate template;

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

}
