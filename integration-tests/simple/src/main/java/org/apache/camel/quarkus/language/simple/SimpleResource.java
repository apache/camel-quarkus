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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;

@Path("/simple")
@ApplicationScoped
public class SimpleResource {

    @Inject
    ProducerTemplate template;

    @Path("/filter")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String filter(boolean premium) {
        return template.requestBodyAndHeader("direct:filter-simple", "NOT-PREMIUM", "premium", premium, String.class);
    }

    @Path("/transform")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String transform(String user) {
        return template.requestBodyAndHeader("direct:transform-simple", null, "user", user, String.class);
    }

    @Path("/resource")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String resource(String body) {
        return template.requestBody("direct:resource-simple", body, String.class);
    }

    @Path("/mandatoryBodyAs")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String mandatoryBodyAs(byte[] body) {
        return template.requestBody("direct:mandatoryBodyAs-simple", body, String.class);
    }

    @Path("/bodyIs")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String mandatoryBodyAs(String body) {
        if ("A body of type String".equals(body)) {
            return template.requestBody("direct:bodyIs-simple", "STRING", String.class);
        } else {
            return template.requestBody("direct:bodyIs-simple", ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8)),
                    String.class);
        }
    }
}
