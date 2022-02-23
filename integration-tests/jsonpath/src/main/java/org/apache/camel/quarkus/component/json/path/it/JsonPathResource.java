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
package org.apache.camel.quarkus.component.json.path.it;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.logging.Logger;

import static org.apache.camel.jsonpath.JsonPathConstants.HEADER_JSON_ENCODING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Path("/jsonpath")
@ApplicationScoped
public class JsonPathResource {

    private static final Logger LOG = Logger.getLogger(JsonPathResource.class);

    private static final String WRITE_AS_STRING_TEST_DATA = "{\"testjson\":{\"users\":[{\"name\":\"Jan\",\"age\":28},{\"age\":10},{\"name\":\"Tom\",\"age\":50}],\"boolean\":true,\"color\":\"gold\","
            + "\"null\":null,\"number\":123,\"object\":{\"objectX\":\"myObjectX\",\"objectY\":\"secondbestobject\","
            + "\"subObject\":{\"obj1\":\"obj1desc\"}},\"string\":\"HelloWorld\"}}";

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/getBookPriceLevel")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getBookPriceLevel(String storeRequestJson) {
        LOG.debugf("Getting book price level from json store request: %s", storeRequestJson);
        return producerTemplate.requestBody("direct:getBookPriceLevel", storeRequestJson, String.class);
    }

    @Path("/getBookPrice")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getBookPrice(String storeRequestJson) {
        LOG.debugf("Getting book price from json store request: %s", storeRequestJson);
        return producerTemplate.requestBody("direct:getBookPrice", storeRequestJson, String.class);
    }

    @Path("/getFullName")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getFullName(String personRequestJson) {
        LOG.debugf("Getting person full name from json person request: %s", personRequestJson);
        return producerTemplate.requestBody("direct:getFullName", personRequestJson, String.class);
    }

    @Path("/getAllCarColors")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getAllCarColors(String carsRequestJson) {
        LOG.debugf("Getting all car colors from json cars request: %s", carsRequestJson);
        return producerTemplate.requestBody("direct:getAllCarColors", carsRequestJson, String.class);
    }

    @Path("/splitBooksShouldReturnTwoPrices")
    @GET
    public void splitBooksShouldReturnThreePrices() throws InterruptedException {
        LOG.debugf("Calling splitBooksShouldReturnThreePrices()");

        String body = "{\"books\": [{\"price\": 30},{ \"price\": 20}]}";
        MockEndpoint mockPrices = context.getEndpoint("mock:prices", MockEndpoint.class);
        mockPrices.expectedMessageCount(2);
        producerTemplate.requestBody("direct:splitBooks", body, String.class);
        mockPrices.assertIsSatisfied();

        List<Exchange> exchanges = mockPrices.getReceivedExchanges();
        assertNotNull(exchanges);
        assertEquals(2, exchanges.size());

        Map<?, ?> firstRow = exchanges.get(0).getIn().getBody(Map.class);
        assertNotNull(firstRow);
        assertEquals(30, firstRow.get("price"));

        Map<?, ?> secondRow = exchanges.get(1).getIn().getBody(Map.class);
        assertNotNull(secondRow);
        assertEquals(20, secondRow.get("price"));
    }

    @Path("/setHeaderWithJsonPathExpressionEvaluatingAnotherHeaderShouldSucceed")
    @GET
    public void setHeaderWithJsonPathExpressionEvaluatingAnotherHeaderShouldSucceed() throws InterruptedException {
        LOG.debugf("Calling setHeaderWithJsonPathExpressionEvaluatingAnotherHeaderShouldSucceed()");

        String json = "{\"book\": {\"price\": 25} }";
        MockEndpoint mockSetHeader = context.getEndpoint("mock:setHeader", MockEndpoint.class);
        mockSetHeader.expectedMessageCount(1);
        mockSetHeader.expectedHeaderReceived("price", 25);
        producerTemplate.requestBodyAndHeader("direct:setHeader", null, "jsonBookHeader", json, Message.class);
        mockSetHeader.assertIsSatisfied();
    }

    @Path("/getAuthorsFromJsonStream")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public List<?> getAuthorsFromJsonStream(byte[] jsonBytes, @QueryParam("encoding") String encoding) throws IOException {
        LOG.debugf("Getting authors from JsonStream with encoding '%s' and %d bytes", encoding, jsonBytes.length);

        try (ByteArrayInputStream jsonStream = new ByteArrayInputStream(jsonBytes)) {
            if (encoding == null) {
                return producerTemplate.requestBody("direct:getAuthorsFromJsonStream", jsonStream, List.class);
            } else {
                return producerTemplate.requestBodyAndHeader("direct:getAuthorsFromJsonStream", jsonStream,
                        HEADER_JSON_ENCODING, encoding, List.class);
            }
        }
    }

    @Path("/splitInputJsonThenWriteAsStringShouldSucceed")
    @GET
    public void splitInputJsonThenWriteAsStringShouldSucceed() throws InterruptedException {
        LOG.debugf("Split input json and then use jsonpath writeAsString");

        MockEndpoint mockJsonpathWriteAsString = context.getEndpoint("mock:jsonpathWriteAsString", MockEndpoint.class);
        mockJsonpathWriteAsString.expectedMessageCount(3);
        List<String> expectedBodies = new ArrayList<>();
        expectedBodies.add("{\"name\":\"Jan\",\"age\":28}");
        expectedBodies.add("{\"age\":10}");
        expectedBodies.add("{\"name\":\"Tom\",\"age\":50}");
        mockJsonpathWriteAsString.expectedBodiesReceived(expectedBodies);
        producerTemplate.requestBody("direct:splitInputJsonThenWriteAsString", WRITE_AS_STRING_TEST_DATA, String.class);
        mockJsonpathWriteAsString.assertIsSatisfied();
    }
}
