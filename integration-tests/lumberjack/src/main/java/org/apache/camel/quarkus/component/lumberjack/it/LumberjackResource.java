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
package org.apache.camel.quarkus.component.lumberjack.it;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.logging.Logger;

@Path("/lumberjack")
@ApplicationScoped
public class LumberjackResource {

    private static final Logger LOG = Logger.getLogger(LumberjackResource.class);

    @Inject
    CamelContext context;

    @Path("results/ssl/none")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public LumberjackResponse getMessagesOfWithoutSsl() throws InterruptedException {
        return getLumberjackResponse(LumberjackRoutes.MOCK_ENDPOINT_WITHOUT_SSL);
    }

    @Path("results/ssl/route")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public LumberjackResponse getMessagesOfWithSsl() throws InterruptedException {
        return getLumberjackResponse(LumberjackRoutes.MOCK_ENDPOINT_WITH_SSL);
    }

    @Path("results/ssl/global")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public LumberjackResponse getMessagesOfWithGlobalSsl() throws InterruptedException {
        return getLumberjackResponse(LumberjackRoutes.MOCK_ENDPOINT_WITH_GLOBAL_SSL);
    }

    private LumberjackResponse getLumberjackResponse(String endpointName) {
        LOG.infof("getting response from mock endpoint %s", endpointName);
        MockEndpoint mockEndpoint = context.getEndpoint(endpointName, MockEndpoint.class);
        return new LumberjackResponse(extractDataFromMock(mockEndpoint));
    }

    private List<Map<String, Map>> extractDataFromMock(MockEndpoint mockEndpoint) {
        List<Map<String, Map>> data = mockEndpoint.getReceivedExchanges().stream().sequential()
                .map(exchange -> {
                    Map<String, Map> map = exchange.getIn().getBody(Map.class);
                    return map;
                })
                .collect(Collectors.toList());
        return data;
    }
}
