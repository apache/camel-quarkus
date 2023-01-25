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
package org.apache.camel.quarkus.component.cbor.it;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.cbor.it.model.Author;
import org.apache.camel.quarkus.component.cbor.it.model.DummyObject;
import org.jboss.logging.Logger;

import static java.util.stream.Collectors.toList;

@Path("/cbor")
@ApplicationScoped
public class CborResource {

    private static final Logger LOG = Logger.getLogger(CborResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("/marshalUnmarshalMap")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<?, ?> marshalUnmarshalMap(Map<String, String> in) {
        LOG.debug("Calling marshalUnmarshalMap(...)");
        Object marshalled = producerTemplate.requestBody("direct:marshal-map", in);
        return producerTemplate.requestBody("direct:unmarshal-map", marshalled, Map.class);
    }

    @Path("/marshalUnmarshalAuthor")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Author marshalUnmarshalAuthor(Author author) {
        LOG.debug("Calling marshalUnmarshalAuthor(...)");
        Object marshalled = producerTemplate.requestBody("direct:marshal-author", author);
        return producerTemplate.requestBody("direct:unmarshal-author", marshalled, Author.class);
    }

    @Path("/marshalUnmarshalCborMethod")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Author marshalUnmarshalCborMethod(Author author) {
        LOG.debug("Calling marshalUnmarshalAuthor(...)");
        Object marshalled = producerTemplate.requestBody("direct:marshalCborMethod", author);
        return producerTemplate.requestBody("direct:unmarshalCborMethod", marshalled, Author.class);
    }

    @Path("/unmarshalAuthorViaJmsTypeHeader")
    @GET
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Author unmarshalAuthorViaJmsTypeHeader(byte[] authorCborBytes) {
        LOG.debug("Calling unmarshalAuthorViaJmsTypeHeader(...)");
        String uri = "direct:unmarshal-via-jms-type-header";
        return producerTemplate.requestBodyAndHeader(uri, authorCborBytes, "JMSType", Author.class.getName(), Author.class);
    }

    @Path("/unmarshalDummyObjectList")
    @GET
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DummyObject> unmarshalDummyObjectList(byte[] dummyObjectListCborBytes) {
        LOG.debug("Calling unmarshalDummyObjectList(...)");
        MockEndpoint mockResult = context.getEndpoint("mock:unmarshal-dummy-object-list", MockEndpoint.class);
        producerTemplate.requestBody("direct:unmarshal-dummy-object-list", dummyObjectListCborBytes);
        return mockResult.getExchanges().stream().map(e -> e.getIn().getBody(DummyObject.class)).collect(toList());
    }
}
