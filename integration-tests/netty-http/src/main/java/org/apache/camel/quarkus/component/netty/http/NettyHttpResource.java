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
package org.apache.camel.quarkus.component.netty.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import io.netty.handler.codec.http.FullHttpResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.netty.http.NettyHttpMessage;
import org.eclipse.microprofile.config.ConfigProvider;

@Path("/netty/http")
public class NettyHttpResource {
    @Inject
    ProducerTemplate producerTemplate;

    @GET
    @Path("/getRequest/{method}/{hName}/{hValue}/{body}")
    public String getRequest(@PathParam("method") String method, @PathParam("hName") String headerName,
            @PathParam("hValue") String headerValue,
            @PathParam("body") String body) {
        return producerTemplate.requestBodyAndHeaders(
                "netty-http:http://localhost:{{camel.netty-http.port}}/request",
                body,
                Map.of(Exchange.HTTP_METHOD, method, headerName, headerValue),
                String.class);
    }

    @GET
    @Path("/getResponse/{message}")
    public String getResponse(@PathParam("message") String message) {
        final Exchange exchange = producerTemplate
                .send("netty-http:http://localhost:{{camel.netty-http.port}}/response", ex -> {
                    ex.getIn().setBody(message);
                });
        FullHttpResponse response = exchange.getIn().getBody(NettyHttpMessage.class).getHttpResponse();
        String received = exchange.getIn().getBody(String.class);
        return received + ": " + response.status().reasonPhrase() + " " + response.status().code();
    }

    @GET
    @Path("/auth/{path}/{user}/{password}")
    public Response auth(@PathParam("path") String path, @PathParam("user") String user,
            @PathParam("password") String password) {
        final Exchange exchange = producerTemplate
                .send("netty-http:http://localhost:{{camel.netty-http.port}}/" + path,
                        ex -> ex.getIn().setHeaders(getAuthHeaders(user, password)));

        return Response.status(exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class)).build();
    }

    @GET
    @Path("/jaas/{user}/{password}")
    public Response auth(@PathParam("user") String user, @PathParam("password") String password) {
        final Exchange exchange = producerTemplate
                .send("netty-http:http://localhost:{{camel.netty-http.port}}/jaas",
                        ex -> ex.getIn().setHeaders(getAuthHeaders(user, password)));

        return Response.status(exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class)).build();
    }

    @GET
    @Path("/wildcard/{path}")
    public Response wildcard(@PathParam("path") String path) {
        final String body = producerTemplate.requestBodyAndHeader(
                "netty-http:http://localhost:{{camel.netty-http.port}}/" + path,
                null, Exchange.HTTP_METHOD, "GET", String.class);
        return Response.ok(body).build();
    }

    @GET
    @Path("/proxy")
    public String proxy() {
        final int proxyPort = ConfigProvider.getConfig().getValue("camel.netty-http.proxyPort", Integer.class);
        final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", proxyPort));

        final int port = ConfigProvider.getConfig().getValue("camel.netty-http.port", Integer.class);
        final String url = "http://localhost:" + port + "/proxy";

        try {
            final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection(proxy);
            return new String(connection.getInputStream().readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/rest/{method}")
    public String rest(@PathParam("method") String method) {
        return producerTemplate.requestBodyAndHeader(
                "netty-http:http://localhost:{{camel.netty-http.restPort}}/rest",
                null,
                Exchange.HTTP_METHOD, method,
                String.class);
    }

    @GET
    @Path("/rest/pojo/{type}")
    public String restPojo(@PathParam("type") String type) {
        final String body;
        final String contentType;
        if ("json".equals(type)) {
            body = "{\"firstName\":\"John\", \"lastName\":\"Doe\"}";
            contentType = "application/json";
        } else {
            body = "<user firstName=\"John\" lastName=\"Doe\"/>";
            contentType = "text/xml";
        }
        return producerTemplate.requestBodyAndHeaders(
                "netty-http:http://localhost:{{camel.netty-http.restPort}}/rest/" + type,
                body,
                Map.of(Exchange.HTTP_METHOD, "POST", "Content-Type", contentType),
                String.class);
    }

    private Map<String, Object> getAuthHeaders(String user, String password) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, "GET");
        if (!"null".equals(user)) {
            headers.put("Authorization", "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes()));
        }
        return headers;
    }
}
