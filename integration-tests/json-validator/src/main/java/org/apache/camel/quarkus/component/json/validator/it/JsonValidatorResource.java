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
package org.apache.camel.quarkus.component.json.validator.it;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/json-validator")
@ApplicationScoped
public class JsonValidatorResource {

    private static final Logger LOG = Logger.getLogger(JsonValidatorResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/validate")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String validate(String json) {
        LOG.infof("Calling validate with: %s", json);
        return producerTemplate.requestBody("direct:validate-json", json, String.class);
    }

    @Path("/validate-as-stream")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String validateAsStream(String json) {
        LOG.infof("Calling validateAsStream with: %s", json);
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        return producerTemplate.requestBody("direct:validate-json", bais, String.class) + "-as-stream";
    }

    @Path("/validate-from-header")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String validateFromHeader(String json) {
        LOG.infof("Calling validateFromHeader with: %s", json);
        return producerTemplate.requestBodyAndHeader("direct:validate-json-from-header", null, "headerToValidate",
                json, String.class);
    }
}
