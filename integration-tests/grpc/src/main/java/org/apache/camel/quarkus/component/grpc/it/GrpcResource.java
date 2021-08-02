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
package org.apache.camel.quarkus.component.grpc.it;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.grpc.it.model.PingRequest;
import org.apache.camel.quarkus.component.grpc.it.model.PongResponse;

import static org.apache.camel.component.grpc.GrpcConstants.GRPC_EVENT_TYPE_HEADER;
import static org.apache.camel.component.grpc.GrpcConstants.GRPC_EVENT_TYPE_ON_COMPLETED;
import static org.apache.camel.component.grpc.GrpcConstants.GRPC_EVENT_TYPE_ON_ERROR;
import static org.apache.camel.component.grpc.GrpcConstants.GRPC_EVENT_TYPE_ON_NEXT;
import static org.apache.camel.component.grpc.GrpcConstants.GRPC_METHOD_NAME_HEADER;
import static org.apache.camel.quarkus.component.grpc.it.GrpcRoute.PING_PONG_SERVICE;

@Path("/grpc")
@ApplicationScoped
public class GrpcResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("/producer")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String producer(String pingName, @QueryParam("pingId") int pingId) throws Exception {
        final PingRequest pingRequest = PingRequest.newBuilder()
                .setPingName(pingName)
                .setPingId(pingId)
                .build();
        final PongResponse response = producerTemplate.requestBody(
                "grpc://localhost:{{grpc.test.server.port}}/" + PING_PONG_SERVICE + "?method=pingSyncSync&synchronous=true",
                pingRequest, PongResponse.class);
        return response.getPongName();
    }

    @Path("/forwardOnCompleted")
    @GET
    public void forwardOnCompleted() throws InterruptedException {
        MockEndpoint endpoint = context.getEndpoint("mock:forwardOnCompleted", MockEndpoint.class);
        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderValuesReceivedInAnyOrder(GRPC_EVENT_TYPE_HEADER, GRPC_EVENT_TYPE_ON_COMPLETED);
        endpoint.expectedHeaderValuesReceivedInAnyOrder(GRPC_METHOD_NAME_HEADER, "pingAsyncAsync");
        endpoint.assertIsSatisfied(5000L);
    }

    @Path("/forwardOnError")
    @GET
    public String forwardOnError() throws InterruptedException {
        MockEndpoint endpoint = context.getEndpoint("mock:forwardOnError", MockEndpoint.class);
        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderValuesReceivedInAnyOrder(GRPC_EVENT_TYPE_HEADER, GRPC_EVENT_TYPE_ON_ERROR);
        endpoint.expectedHeaderValuesReceivedInAnyOrder(GRPC_METHOD_NAME_HEADER, "pingAsyncAsync");
        endpoint.assertIsSatisfied(5000L);

        List<Exchange> exchanges = endpoint.getExchanges();
        Exchange exchange = exchanges.get(0);
        Throwable throwable = exchange.getMessage().getBody(Throwable.class);
        return throwable.getClass().getName();
    }

    @Path("/grpcStreamReplies")
    @GET
    public void grpcStreamReplies() throws InterruptedException {
        int messageCount = 10;
        for (int i = 1; i <= messageCount; i++) {
            PingRequest request = PingRequest.newBuilder().setPingName(String.valueOf(i)).build();
            producerTemplate.sendBody("direct:grpcStream", request);
        }

        MockEndpoint endpoint = context.getEndpoint("mock:grpcStreamReplies", MockEndpoint.class);
        endpoint.expectedMessageCount(messageCount);
        endpoint.assertIsSatisfied();
    }

    @Path("/tls")
    @GET
    public void tlsConsumer() throws InterruptedException {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:tls", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(GRPC_EVENT_TYPE_HEADER, GRPC_EVENT_TYPE_ON_NEXT);
        mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(GRPC_METHOD_NAME_HEADER, "pingAsyncSync");
        mockEndpoint.assertIsSatisfied();
    }

    @Path("/tls")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String tlsProducer(String message) {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:tls", MockEndpoint.class);
        try {
            PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName(message)
                    .setPingId(12345)
                    .build();

            PongResponse response = producerTemplate.requestBody("direct:sendTls", pingRequest, PongResponse.class);
            return response.getPongName();
        } finally {
            mockEndpoint.reset();
        }
    }

    @Path("/jwt")
    @GET
    public void jwtConsumer() throws InterruptedException {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:jwt", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(GRPC_EVENT_TYPE_HEADER, GRPC_EVENT_TYPE_ON_NEXT);
        mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(GRPC_METHOD_NAME_HEADER, "pingAsyncSync");
        mockEndpoint.assertIsSatisfied();
    }

    @Path("/jwt")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String jwtProducer(String message) {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:jwt", MockEndpoint.class);
        try {
            PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName(message)
                    .setPingId(12345)
                    .build();

            PongResponse response = producerTemplate.requestBody("direct:sendJwt", pingRequest, PongResponse.class);
            return response.getPongName();
        } finally {
            mockEndpoint.reset();
        }
    }
}
