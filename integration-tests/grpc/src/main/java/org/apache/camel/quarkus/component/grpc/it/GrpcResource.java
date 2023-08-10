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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.grpc.it.model.PingRequest;
import org.apache.camel.quarkus.component.grpc.it.model.PongResponse;

import static org.apache.camel.component.grpc.GrpcConstants.GRPC_METHOD_NAME_HEADER;

@Path("/grpc")
@ApplicationScoped
public class GrpcResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("/producer")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public String producer(
            @QueryParam("portPropertyPlaceholder") String portPropertyPlaceholder,
            @QueryParam("pingName") String pingName,
            @QueryParam("methodName") String methodName,
            @QueryParam("pingId") int pingId,
            @QueryParam("synchronous") boolean synchronous) {

        final PingRequest pingRequest = PingRequest.newBuilder()
                .setPingName(pingName)
                .setPingId(pingId)
                .build();
        final Map<String, Object> headers = Map.of(
                "port", portPropertyPlaceholder,
                GRPC_METHOD_NAME_HEADER, methodName,
                "isSync", String.valueOf(synchronous));

        PongResponse response;
        if (methodName.contains("Async")) {
            List<PongResponse> responses = producerTemplate.requestBodyAndHeaders("direct:sendGrpcMessage", pingRequest,
                    headers, List.class);
            response = responses.get(0);
        } else {
            response = producerTemplate.requestBodyAndHeaders("direct:sendGrpcMessage", pingRequest, headers,
                    PongResponse.class);
        }
        return "{\"pongName\": \"%s\", \"pongId\": %d}".formatted(response.getPongName(), response.getPongId());
    }

    @Path("/forwardOnCompleted")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> forwardOnCompleted() throws InterruptedException {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:forwardOnCompleted", MockEndpoint.class);
        List<Exchange> exchanges = mockEndpoint.getExchanges();
        if (!exchanges.isEmpty()) {
            Exchange exchange = exchanges.get(0);
            return exchange.getMessage().getHeaders();
        }
        return Collections.emptyMap();
    }

    @Path("/forwardOnError")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> forwardOnError() throws InterruptedException {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:forwardOnError", MockEndpoint.class);
        List<Exchange> exchanges = mockEndpoint.getExchanges();
        if (!exchanges.isEmpty()) {
            Exchange exchange = exchanges.get(0);
            Throwable throwable = exchange.getMessage().getBody(Throwable.class);
            Map<String, Object> results = exchange.getMessage().getHeaders();
            results.put("error", throwable.getClass().getName());
            return results;
        }
        return Collections.emptyMap();
    }

    @Path("/grpcStreamReplies")
    @GET
    public void grpcStreamReplies() throws InterruptedException {
        int messageCount = 10;
        MockEndpoint endpoint = context.getEndpoint("mock:grpcStreamReplies", MockEndpoint.class);
        endpoint.expectedMessageCount(messageCount);

        for (int i = 1; i <= messageCount; i++) {
            PingRequest request = PingRequest.newBuilder().setPingName(String.valueOf(i)).build();
            producerTemplate.sendBody("direct:grpcStream", request);
        }

        endpoint.assertIsSatisfied();
    }

    @Path("/tls")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> tlsConsumer() throws InterruptedException {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:tls", MockEndpoint.class);
        List<Exchange> exchanges = mockEndpoint.getExchanges();
        if (!exchanges.isEmpty()) {
            Exchange exchange = exchanges.get(0);
            return exchange.getMessage().getHeaders();
        }
        return Collections.emptyMap();
    }

    @Path("/tls")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String tlsProducer(String message) {
        PingRequest pingRequest = PingRequest.newBuilder()
                .setPingName(message)
                .setPingId(12345)
                .build();

        PongResponse response = producerTemplate.requestBodyAndHeader(
                "direct:sendTls",
                pingRequest,
                "origin",
                "producer",
                PongResponse.class);
        return response.getPongName();
    }

    @Path("/jwt")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> jwtConsumer() throws InterruptedException {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:jwt", MockEndpoint.class);
        List<Exchange> exchanges = mockEndpoint.getExchanges();
        if (!exchanges.isEmpty()) {
            Exchange exchange = exchanges.get(0);
            return exchange.getMessage().getHeaders();
        }
        return Collections.emptyMap();
    }

    @Path("/jwt")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String jwtProducer(String message) {
        PingRequest pingRequest = PingRequest.newBuilder()
                .setPingName(message)
                .setPingId(12345)
                .build();

        PongResponse response = producerTemplate.requestBodyAndHeader(
                "direct:sendJwt",
                pingRequest,
                "origin",
                "producer",
                PongResponse.class);
        return response.getPongName();
    }
}
