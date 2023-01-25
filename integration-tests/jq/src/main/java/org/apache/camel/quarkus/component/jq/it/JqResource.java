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
package org.apache.camel.quarkus.component.jq.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;

@Path("/jq")
@ApplicationScoped
public class JqResource {
    @Inject
    ProducerTemplate producerTemplate;
    @Inject
    CamelContext context;
    @Inject
    ObjectMapper mapper;

    @Path("/expression")
    @GET
    public void expression() throws Exception {
        MockEndpoint endpoint = context.getEndpoint("mock:expression", MockEndpoint.class);
        endpoint.expectedBodiesReceived(new TextNode("bar"));

        ObjectNode node = mapper.createObjectNode()
                .put("foo", "bar")
                .put("baz", "bar");

        producerTemplate.sendBody("direct:expression", node.put("value", "valid"));

        endpoint.assertIsSatisfied(5000);
    }

    @Path("/expression/header")
    @GET
    public void expressionHeader() throws Exception {
        MockEndpoint endpoint = context.getEndpoint("mock:expressionHeader", MockEndpoint.class);
        TextNode expected = new TextNode("bar");
        endpoint.expectedBodiesReceived(expected);

        ObjectNode node = mapper.createObjectNode().put("foo", "bar");
        producerTemplate.sendBodyAndHeader("direct:expressionHeader", null, "Content", node);

        endpoint.assertIsSatisfied(5000);
    }

    @Path("/expression/header/function")
    @GET
    public void expressionHeaderFunction() throws Exception {
        MockEndpoint endpoint = context.getEndpoint("mock:expressionHeaderFunction", MockEndpoint.class);
        ObjectNode expected = mapper.createObjectNode().put("foo", "MyValue");
        endpoint.expectedBodiesReceived(expected);

        ObjectNode node = mapper.createObjectNode().put("foo", "bar");
        producerTemplate.sendBodyAndHeader("direct:expressionHeaderFunction", node, "MyHeader", "MyValue");

        endpoint.assertIsSatisfied(5000);
    }

    @Path("/expression/header/string")
    @GET
    public void expressionHeaderString() throws Exception {
        MockEndpoint endpoint = context.getEndpoint("mock:expressionHeaderString", MockEndpoint.class);
        endpoint.expectedBodiesReceived("bar");

        ObjectNode node = mapper.createObjectNode().put("foo", "bar");
        producerTemplate.sendBodyAndHeader("direct:expressionHeaderString", null, "Content", node);

        endpoint.assertIsSatisfied(5000);
    }

    @Path("/expression/pojo")
    @GET
    public void expressionPojo() throws Exception {
        MockEndpoint endpoint = context.getEndpoint("mock:expressionPojo", MockEndpoint.class);
        endpoint.expectedBodiesReceived(new Book("foo", "bar"));

        ObjectNode node = mapper.createObjectNode();
        node.with("book").put("author", "foo").put("title", "bar");

        producerTemplate.sendBody("direct:expressionPojo", node);

        endpoint.assertIsSatisfied(5000);
    }

    @Path("/expression/property")
    @GET
    public void expressionProperty() throws Exception {
        MockEndpoint endpoint = context.getEndpoint("mock:expressionProperty", MockEndpoint.class);
        endpoint.expectedMessageCount(1);

        ObjectNode node = mapper.createObjectNode().put("foo", "bar");

        producerTemplate.send("direct:expressionProperty", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.setProperty("Content", node);
            }
        });

        endpoint.assertIsSatisfied(5000);
    }

    @Path("/expression/property/function")
    @GET
    public void expressionPropertyFunction() throws Exception {
        ObjectNode node = mapper.createObjectNode().put("foo", "MyPropertyValue");
        MockEndpoint endpoint = context.getEndpoint("mock:expressionPropertyFunction", MockEndpoint.class);
        endpoint.expectedBodiesReceived(node);

        producerTemplate.send("direct:expressionPropertyFunction", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.setProperty("MyProperty", "MyPropertyValue");
                exchange.getMessage().setBody(mapper.createObjectNode().put("foo", "bar"));
            }
        });

        endpoint.assertIsSatisfied(5000);
    }

    @Path("/filter")
    @GET
    public void filter() throws Exception {
        MockEndpoint endpoint = context.getEndpoint("mock:filter", MockEndpoint.class);
        ObjectNode expected = mapper.createObjectNode().put("value", "valid");
        endpoint.expectedBodiesReceived(expected);

        producerTemplate.sendBody("direct:filter", mapper.createObjectNode().put("value", "valid"));
        producerTemplate.sendBody("direct:filter", mapper.createObjectNode().put("value", "invalid"));

        endpoint.assertIsSatisfied(5000);
    }
}
