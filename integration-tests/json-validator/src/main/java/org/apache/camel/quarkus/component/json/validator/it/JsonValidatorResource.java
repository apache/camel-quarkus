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
import java.util.List;

import com.networknt.schema.ValidationMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jsonvalidator.JsonValidationException;
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
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> validate(String json) {
        try {
            LOG.infof("Calling validate with: %s", json);
            String result = producerTemplate.requestBody("direct:validate-json", json, String.class);
            return List.of(result);
        } catch (CamelExecutionException e) {
            if (e.getCause() instanceof JsonValidationException jve) {
                return jve.getErrors()
                        .stream()
                        .map(ValidationMessage::getError)
                        .toList();
            } else {
                throw e;
            }
        }
    }

    @Path("/validate-as-stream")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> validateAsStream(String json) {
        try {
            LOG.infof("Calling validateAsStream with: %s", json);
            ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            String result = producerTemplate.requestBody("direct:validate-json", bais, String.class) + "-as-stream";
            return List.of(result);
        } catch (CamelExecutionException e) {
            if (e.getCause() instanceof JsonValidationException jve) {
                return jve.getErrors()
                        .stream()
                        .map(ValidationMessage::getError)
                        .toList();
            } else {
                throw e;
            }
        }
    }

    @Path("/validate-from-header")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> validateFromHeader(String json) {
        try {
            LOG.infof("Calling validateFromHeader with: %s", json);
            String result = producerTemplate.requestBodyAndHeader("direct:validate-json-from-header", null, "headerToValidate",
                    json, String.class);
            return List.of(result);
        } catch (CamelExecutionException e) {
            if (e.getCause() instanceof JsonValidationException jve) {
                return jve.getErrors()
                        .stream()
                        .map(ValidationMessage::getError)
                        .toList();
            } else {
                throw e;
            }
        }
    }
}
