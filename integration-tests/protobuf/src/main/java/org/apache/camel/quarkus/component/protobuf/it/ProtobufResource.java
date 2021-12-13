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
package org.apache.camel.quarkus.component.protobuf.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.protobuf.it.model.AddressBookProtos.Person;

@Path("/protobuf")
@ApplicationScoped
public class ProtobufResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/marshal")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] marshal(@QueryParam("id") int id, @QueryParam("name") String name) {
        final Person person = Person.newBuilder()
                .setId(id)
                .setName(name)
                .build();
        return producerTemplate.requestBody("direct:protobuf-marshal", person, byte[].class);
    }

    @Path("/unmarshal")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String unmarshal(byte[] body) {
        final Person person = producerTemplate.requestBody("direct:protobuf-unmarshal", body, Person.class);
        return "{\"name\": \"" + person.getName() + "\",\"id\": " + person.getId() + "}";
    }

    @Path("/marshal-json")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String marshalJson(@QueryParam("id") int id, @QueryParam("name") String name) {
        final Person person = Person.newBuilder()
                .setId(id)
                .setName(name)
                .build();
        return producerTemplate.requestBody("direct:protobuf-marshal-json", person, String.class);
    }

    @Path("/unmarshal-json")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String unmarshalJson(String body) {
        final Person person = producerTemplate.requestBody("direct:protobuf-unmarshal-json", body, Person.class);
        return person.getName() + " - " + person.getId();
    }

}
