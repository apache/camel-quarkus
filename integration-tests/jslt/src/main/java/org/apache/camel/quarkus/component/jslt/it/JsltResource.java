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
package org.apache.camel.quarkus.component.jslt.it;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jslt.JsltConstants;
import org.jboss.logging.Logger;

import static java.util.Collections.singletonMap;

@Path("/jslt")
@ApplicationScoped
public class JsltResource {

    private static final Logger LOG = Logger.getLogger(JsltResource.class);

    @Inject
    ProducerTemplate template;

    @Path("/transformInputStream")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String transformInputStream(String input) throws IOException {
        LOG.debugf("Invoking transformInputStream(%s)", input);
        try (InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {
            return template.requestBody("jslt:demoPlayground/transformation.json", is, String.class);
        }
    }

    @Path("/transformInvalidBody")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String transformInvalidBody() {
        LOG.debugf("Invoking transformInvalidBody()");
        try {
            template.requestBody("jslt:demoPlayground/transformation.json", 4, String.class);
        } catch (CamelExecutionException vex) {
            return vex.getCause().getMessage();
        }
        return null;
    }

    @Path("/transformString")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String transformString(String input) {
        LOG.debugf("Invoking transformString(%s)", input);
        return template.requestBody("jslt:demoPlayground/transformation.json", input, String.class);
    }

    @Path("/transformFromHeaderWithPrettyPrint")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String transformFromHeaderWithPrettyPrint(String input) throws IOException {
        LOG.debugf("Invoking transformFromHeaderWithPrettyPrint(%s)", input);
        Map<String, Object> headers = singletonMap(JsltConstants.HEADER_JSLT_RESOURCE_URI,
                "demoPlayground/transformation.json");
        try (InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {
            String uri = "jslt:demoPlayground/transformation.json?prettyPrint=true&allowTemplateFromHeader=true";
            return template.requestBodyAndHeaders(uri, is, headers, String.class);
        }
    }

    @Path("/transformInputStreamWithFilter")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String transformInputStreamWithFilter(String input) throws IOException {
        LOG.debugf("Invoking transformInputStreamWithFilter(%s)", input);
        try (InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {
            String uri = "jsltWithFilter:objectFilter/transformation.json";
            return template.requestBody(uri, is, String.class);
        }
    }

    @Path("/transformInputStreamWithVariables")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String transformInputStreamWithVariables(String input) throws IOException {
        LOG.debugf("Invoking transformInputStreamWithVariables(%s)", input);

        Map<String, Object> headers = new HashMap<>();
        headers.put("published", "2020-05-26T16:00:00+02:00");
        headers.put("type", "Controller");
        // infinite recursion value that cannot be serialized by camel-jslt
        headers.put("infinite", JsltConfiguration.createInfiniteRecursionObject());

        try (InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {
            String uri = "jslt:withVariables/transformation.json";
            return template.requestBodyAndHeaders(uri, is, headers, String.class);
        }
    }

    @Path("/transformInputStreamWithVariablesAndProperties")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String transformInputStreamWithVariablesAndProperties(String input) throws IOException {
        LOG.debugf("Invoking transformInputStreamWithVariablesAndProperties(%s)", input);

        try (InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {
            String uri = "jslt:withVariables/transformationWithProperties.json?allowContextMapAll=true";

            Exchange ex = template.request(uri, e -> {
                e.getMessage().setBody(is);
                e.getMessage().setHeader("published", "2020-05-26T16:00:00+02:00");
                e.getMessage().setHeader("type", "Controller");
                e.setProperty("infinite", JsltConfiguration.createInfiniteRecursionObject());
                e.setProperty("instance", "559e934f-b32b-47ab-8327-bd50e2bdc029");
            });
            return ex.getMessage().getBody(String.class);
        }
    }

    @Path("/transformWithFunction")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String transformWithFunction() {
        LOG.debugf("Invoking transformWithFunction()");
        Map<String, Object> headers = singletonMap(JsltConstants.HEADER_JSLT_STRING, "power(2, 10)");
        return template.requestBodyAndHeaders("jsltWithFunction:dummy", "{}", headers, String.class);
    }
}
