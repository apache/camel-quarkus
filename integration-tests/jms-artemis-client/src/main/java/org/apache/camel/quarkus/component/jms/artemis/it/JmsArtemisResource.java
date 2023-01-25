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
package org.apache.camel.quarkus.component.jms.artemis.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;

@ApplicationScoped
@Path("/messaging/jms/artemis")
public class JmsArtemisResource {

    @Inject
    ConnectionFactory connectionFactory;

    @Produce("jms:queue:pojoProduce")
    ProducerTemplate pojoProducer;

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @GET
    @Path("/connection/factory")
    @Produces(MediaType.TEXT_PLAIN)
    public String connectionFactoryImplementation() {
        return connectionFactory.getClass().getName();
    }

    @POST
    @Path("/pojo/producer")
    public void pojoProducer(String message) {
        pojoProducer.sendBody(message);
    }

    @POST
    @Path("/xa")
    public String testXA(String message) throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:xaResult", MockEndpoint.class);

        mockEndpoint.reset();
        if (isValid(message)) {
            mockEndpoint.expectedMessageCount(1);
        } else {
            mockEndpoint.expectedMessageCount(0);
        }

        try {
            producerTemplate.sendBody("direct:xa", message);
        } catch (CamelExecutionException e) {
            // ignore the exception and we will check the mock:xaResult
        }
        mockEndpoint.assertIsSatisfied(5000);

        if (isValid(message)) {
            return mockEndpoint.getExchanges().get(0).getIn().getBody(String.class);
        } else {
            return "rollback";
        }
    }

    private boolean isValid(String message) {
        return !message.startsWith("fail");
    }
}
