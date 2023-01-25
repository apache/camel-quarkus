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
package org.apache.camel.quarkus.component.jackson.avro.it;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.avro.AvroMapper;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.apache.avro.Schema;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jackson.SchemaResolver;
import org.apache.camel.component.jackson.avro.JacksonAvroDataFormat;

@Path("/jackson-avro")
@ApplicationScoped
public class JacksonAvroResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/marshal")
    @POST
    public Response avroJacksonMarshal(String message) {
        Pojo pojo = new Pojo(message);
        byte[] result = producerTemplate.requestBody("direct:marshal-pojo", pojo, byte[].class);
        return Response.ok(result).build();
    }

    @Path("/marshal/list")
    @POST
    public Response avroJacksonMarshalForList(String message) {
        List<Pojo> pojos = new ArrayList<>();
        for (String text : message.split(" ")) {
            pojos.add(new Pojo(text));
        }
        byte[] result = producerTemplate.requestBody("direct:marshal-pojo-list", pojos, byte[].class);
        return Response.ok(result).build();
    }

    @Path("/unmarshal/{type}")
    @POST
    @Consumes("application/octet-stream")
    @SuppressWarnings("unchecked")
    public Response avroJacksonUnMarshal(@PathParam("type") String type, byte[] message) {
        Response.ResponseBuilder builder = Response.ok();
        String directUri = "direct:unmarshal-" + type;

        if (type.equals("pojo") || type.equals("defined-dataformat")) {
            Pojo result = producerTemplate.requestBody(directUri, message, Pojo.class);
            builder.entity(result.getText());
        } else if (type.equals("json-node")) {
            JsonNode result = producerTemplate.requestBody(directUri, message, JsonNode.class);
            builder.entity(result.at("/text").asText());
        } else if (type.equals("pojo-list")) {
            List<Pojo> result = producerTemplate.requestBody(directUri, message, List.class);
            StringJoiner joiner = new StringJoiner(" ");
            result.stream()
                    .map(Pojo::getText)
                    .forEach(joiner::add);
            builder.entity(joiner.toString());
        } else {
            throw new IllegalArgumentException("Unknown unmarshal type: " + type);
        }

        return builder.build();
    }

    @Named
    public SchemaResolver avroSchemaResolver() throws IOException {
        return createSchemaResolver("/pojo.avsc");
    }

    @Named
    public JacksonAvroDataFormat jacksonAvroDataFormat() {
        JacksonAvroDataFormat dataFormat = new JacksonAvroDataFormat();
        dataFormat.setAutoDiscoverObjectMapper(true);
        dataFormat.setAutoDiscoverSchemaResolver(true);
        dataFormat.setUnmarshalType(Pojo.class);
        return dataFormat;
    }

    @Named
    public JacksonAvroDataFormat jacksonAvroDataFormatForList() throws IOException {
        SchemaResolver schemaResolver = createSchemaResolver("/pojo-list.avsc");
        JacksonAvroDataFormat dataFormat = new JacksonAvroDataFormat();
        dataFormat.setUnmarshalType(Pojo.class);
        dataFormat.setUseList(true);
        dataFormat.setSchemaResolver(schemaResolver);
        return dataFormat;
    }

    @Named
    public AvroMapper avroMapper() {
        AvroMapper avroMapper = new AvroMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Pojo.class, new StringAppendingDeserializer());
        avroMapper.registerModule(module);
        return avroMapper;
    }

    private SchemaResolver createSchemaResolver(String schemaPath) throws IOException {
        try (InputStream resource = JacksonAvroResource.class.getResourceAsStream(schemaPath)) {
            Schema raw = new Schema.Parser().setValidate(true).parse(resource);
            AvroSchema schema = new AvroSchema(raw);
            return ex -> schema;
        }
    }
}
