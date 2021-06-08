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
package org.apache.camel.quarkus.component.jackson.protobuf.it;

import java.io.IOException;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jackson.SchemaResolver;
import org.apache.camel.component.jackson.protobuf.JacksonProtobufDataFormat;
import org.apache.camel.util.IOHelper;

@Path("/jackson-protobuf")
@ApplicationScoped
public class JacksonProtobufResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/marshal")
    @POST
    public Response protobufJacksonMarshal(String message) throws Exception {
        Pojo pojo = new Pojo(message);
        byte[] result = producerTemplate.requestBody("direct:marshal-pojo", pojo, byte[].class);
        return Response.ok(result).build();
    }

    @Path("/unmarshal/{type}")
    @POST
    @Consumes("application/octet-stream")
    public Response protobufJacksonUnMarshal(@PathParam("type") String type, byte[] message) throws Exception {
        Response.ResponseBuilder builder = Response.ok();
        String directUri = "direct:unmarshal-" + type;

        if (type.equals("pojo") || type.equals("defined-dataformat") || type.equals("quarkus-objectmapper")) {
            Pojo result = producerTemplate.requestBody(directUri, message, Pojo.class);
            builder.entity(result.getText());
        } else if (type.equals("json-node")) {
            JsonNode result = producerTemplate.requestBody(directUri, message, JsonNode.class);
            builder.entity(result.at("/text").asText());
        } else {
            throw new IllegalArgumentException("Unknown unmarshal type: " + type);
        }

        return builder.build();
    }

    @Named
    public SchemaResolver protobufSchemaResolver() throws IOException {
        InputStream resource = JacksonProtobufResource.class.getResourceAsStream("/pojo.proto");
        ProtobufSchema schema = ProtobufSchemaLoader.std.parse(IOHelper.loadText(resource));
        return ex -> schema;
    }

    @Named
    public JacksonProtobufDataFormat jacksonProtobufDataFormat() {
        JacksonProtobufDataFormat dataFormat = new JacksonProtobufDataFormat();
        dataFormat.setAutoDiscoverObjectMapper(true);
        dataFormat.setAutoDiscoverSchemaResolver(true);
        dataFormat.setUnmarshalType(Pojo.class);
        return dataFormat;
    }

    @Named
    public ProtobufMapper protobufMapper() {
        ProtobufMapper protobufMapper = new ProtobufMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Pojo.class, new UppercaseTextDeserializer());
        protobufMapper.registerModule(module);
        return protobufMapper;
    }
}
