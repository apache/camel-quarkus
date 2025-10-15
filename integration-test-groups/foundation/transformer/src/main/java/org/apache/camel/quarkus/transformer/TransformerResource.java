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
package org.apache.camel.quarkus.transformer;

import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spi.TransformerRegistry;

@Path("/transformer")
@ApplicationScoped
public class TransformerResource {

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/registry")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String transformerRegistry() {
        TransformerRegistry transformerRegistry = context.getTransformerRegistry();
        if (transformerRegistry != null) {
            return transformerRegistry.getClass().getName();
        }
        return null;
    }

    @Path("/registry/values")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String transformerRegistryValues() {
        TransformerRegistry transformerRegistry = context.getTransformerRegistry();
        if (transformerRegistry != null) {
            String mapAsString = transformerRegistry.keySet().stream()
                    .map(key -> key + "=" + transformerRegistry.get(key))
                    .collect(Collectors.joining(", ", "{", "}"));
            return mapAsString;
        }
        return null;
    }

    @Path("/toString")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String transformBeanToString(String message) {
        TransformerBean bean = new TransformerBean(message);
        return (String) producerTemplate.requestBody("direct:transformBeanToString", bean);
    }

    @Path("/toBytes")
    @POST
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] transformBeanToBytes(String message) {
        TransformerBean bean = new TransformerBean(message);
        return (byte[]) producerTemplate.requestBody("direct:transformBeanToBytes", bean);
    }

    @Path("/toLowercase")
    @POST
    @Produces({ "plain/lowercase" })
    public String stringToLowercase(String message) {
        return (String) producerTemplate.requestBody("direct:stringToLowercase", message);
    }

    @Path("/toUppercase")
    @POST
    @Produces({ "plain/uppercase" })
    public String stringToUppercase(String message) {
        return (String) producerTemplate.requestBody("direct:stringToUppercase", message);
    }

    @Path("/toJson")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String transformBeanToJson(String message) {
        TransformerBean bean = new TransformerBean(message);
        Object marshalled = producerTemplate.requestBody("direct:transformBeanToJson", bean);
        return context.getTypeConverter().convertTo(String.class, marshalled);
    }

    @Path("/toReversed")
    @POST
    @Produces({ "plain/reversed" })
    public String stringToReversed(String message) {
        return (String) producerTemplate.requestBody("direct:stringToReversed", message);
    }

    @Path("/toTrimmed")
    @POST
    @Produces({ "plain/trimmed" })
    public String stringToTrimmed(String message) {
        return (String) producerTemplate.requestBody("direct:stringToTrimmed", message);
    }
}
