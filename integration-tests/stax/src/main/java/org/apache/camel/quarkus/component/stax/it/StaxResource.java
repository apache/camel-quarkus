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
package org.apache.camel.quarkus.component.stax.it;

import java.io.InputStream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.stax.it.model.Record;

@Path("/stax")
public class StaxResource {

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/records")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int staxProcessRecords() throws Exception {
        try (InputStream resource = StaxResource.class.getResourceAsStream("/records.xml")) {
            TagCountHandler countingHandler = producerTemplate.requestBody("direct:processRecords", resource,
                    TagCountHandler.class);
            return countingHandler.getCount();
        }
    }

    @Path("/records/byref")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int staxProcessRecordsByRef() throws Exception {
        try (InputStream resource = StaxResource.class.getResourceAsStream("/records.xml")) {
            TagCountHandler countingHandler = producerTemplate.requestBody("direct:processRecordsByRef", resource,
                    TagCountHandler.class);
            return countingHandler.getCount();
        }
    }

    @Path("/records/split")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public void staxSplitRecords() throws Exception {
        try (InputStream resource = StaxResource.class.getResourceAsStream("/records.xml")) {
            MockEndpoint mockEndpoint = context.getEndpoint("mock:split", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(10);
            mockEndpoint.allMessages().body().isInstanceOf(Record.class);

            producerTemplate.sendBody("direct:splitRecords", resource);

            mockEndpoint.assertIsSatisfied();
        }
    }

    @jakarta.enterprise.inject.Produces
    @Named("countingHandler")
    public TagCountHandler countingHandler() {
        return new TagCountHandler();
    }
}
