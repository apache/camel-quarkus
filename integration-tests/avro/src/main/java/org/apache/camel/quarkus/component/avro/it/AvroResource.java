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
package org.apache.camel.quarkus.component.avro.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import example.avro.Admin;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.camel.ProducerTemplate;

import static org.apache.camel.quarkus.component.avro.it.AvroSchemaLoader.getSchema;

@Path("/avro")
@ApplicationScoped
public class AvroResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/genericMarshalUnmarshalUsingBuildTimeAvroDataFormat/{value}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String genericMarshalUnmarshalUsingBuildTimeAvroDataFormat(@PathParam("value") String value) {
        GenericRecord input = new GenericData.Record(getSchema());
        input.put("name", value);
        Object marshalled = producerTemplate.requestBody("direct:marshalUsingBuildTimeAvroDataFormat", input);
        GenericRecord output = producerTemplate.requestBody("direct:unmarshalUsingBuildTimeAvroDataFormat", marshalled,
                GenericRecord.class);
        return output.get("name").toString();
    }

    @Path("/genericMarshalUnmarshalUsingBuildTimeGeneratedClass/{value}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String genericMarshalUnmarshalUsingBuildTimeGeneratedClass(@PathParam("value") String value) {
        GenericRecord input = new GenericData.Record(Admin.SCHEMA$);
        input.put("name", value);
        Object marshalled = producerTemplate.requestBody("direct:marshalUsingBuildTimeGeneratedClass", input);
        GenericRecord output = producerTemplate.requestBody("direct:unmarshalUsingBuildTimeGeneratedClass", marshalled,
                GenericRecord.class);
        return output.get("name").toString();
    }

    @Path("/genericMarshalUnmarshalUsingConfigureTimeAvroDataFormat/{value}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String genericMarshalUsingConfigureTimeAvroDataFormat(@PathParam("value") String value) {
        GenericRecord input = new GenericData.Record(getSchema());
        input.put("name", value);
        Object marshalled = producerTemplate.requestBody("direct:marshalUsingConfigureTimeAvroDataFormat", input);
        GenericRecord output = producerTemplate.requestBody("direct:unmarshalUsingConfigureTimeAvroDataFormat", marshalled,
                GenericRecord.class);
        return output.get("name").toString();
    }

    @Path("/valueMarshalUnmarshalUsingInstanceClassNameAvroDsl/{value}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String valueMarshalUnmarshalUsingInstanceClassNameAvroDsl(@PathParam("value") String value) {
        Value input = Value.newBuilder().setValue(value).build();
        Object marshalled = producerTemplate.requestBody("direct:marshalUsingAvroDsl", input);
        Value output = producerTemplate.requestBody("direct:unmarshalUsingInstanceClassNameAvroDsl", marshalled, Value.class);
        return output.getValue().toString();
    }

    @Path("/valueMarshalUnmarshalUsingSchemaAvroDsl/{value}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String valueMarshalUnmarshalUsingSchemaAvroDsl(@PathParam("value") String value) {
        Value input = Value.newBuilder().setValue(value).build();
        Object marshalled = producerTemplate.requestBody("direct:marshalUsingAvroDsl", input);
        Value output = producerTemplate.requestBody("direct:unmarshalUsingSchemaAvroDsl", marshalled, Value.class);
        return output.getValue().toString();
    }
}
