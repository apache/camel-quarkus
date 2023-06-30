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
package org.apache.camel.quarkus.component.http.netty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import io.netty.handler.codec.http.FullHttpResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
import org.apache.camel.component.netty.http.NettyHttpMessage;
import org.apache.camel.quarkus.component.http.common.AbstractHttpResource;
import org.apache.camel.util.IOHelper;

@Path("/test/client/netty-http")
@ApplicationScoped
public class NettyHttpResource extends AbstractHttpResource {
    @Override
    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String httpGet(@QueryParam("test-port") int port) {
        return producerTemplate
                .toF("netty-http:http://localhost:%d/service/common/get?bridgeEndpoint=true", port)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .request(String.class);
    }

    @Override
    @Path("/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String httpPost(@QueryParam("test-port") int port, String message) {
        return producerTemplate
                .toF("netty-http:http://localhost:%d/service/common/toUpper?bridgeEndpoint=true", port)
                .withBody(message)
                .withHeader(Exchange.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .withHeader(Exchange.HTTP_METHOD, "POST")
                .request(String.class);
    }

    @Override
    @Path("/auth/basic")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response basicAuth(@QueryParam("test-port") int port, @QueryParam("username") String username,
            @QueryParam("password") String password) {
        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        Exchange result = producerTemplate
                .withHeader(Exchange.HTTP_QUERY, "component=netty-http")
                .withHeader("Authorization", "Basic " + encoded)
                .toF("netty-http:http://localhost:%d/test/client/netty-http/auth/basic/secured?throwExceptionOnFailure=false",
                        port)
                .send();

        Integer status = result.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String body = result.getMessage().getBody(String.class);
        return Response.status(status).entity(body).build();
    }

    @Override
    @Path("/proxy")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public String httpProxy() {
        String url = String.format(PROXIED_URL, "netty-http");
        return producerTemplate
                .withHeader(Exchange.HTTP_QUERY, "component=netty-http")
                .toF("netty-http:%s?clientInitializerFactory=#proxyCapableClientInitializerFactory", url)
                .request(String.class);
    }

    @Path("/compression")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String compression(@QueryParam("test-port") int port) throws IOException {
        byte[] compressed = producerTemplate
                .toF("netty-http:http://localhost:%d/compressed", port)
                .withHeader("Accept-Encoding", "gzip, deflate")
                .request(byte[].class);

        try (GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return IOHelper.loadText(inputStream).trim();
        }
    }

    @Path("/serialized/exception")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String nettyHttpSerializedException(@QueryParam("test-port") int port) {
        Exchange exchange = producerTemplate
                .toF("netty-http:http://localhost:%d/test/server/serialized/exception?transferException=true", port)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .send();
        return exchange.getException().getClass().getName();
    }

    @GET
    @Path("/getRequest/{method}/{hName}/{hValue}/{body}")
    public String getRequest(@PathParam("method") String method, @PathParam("hName") String headerName,
            @PathParam("hValue") String headerValue,
            @PathParam("body") String body,
            @QueryParam("test-port") int port) {
        return producerTemplate.toF("netty-http:http://localhost:%d/request", port)
                .withHeaders(Map.of(Exchange.HTTP_METHOD, method, headerName, headerValue))
                .withBody(body)
                .request(String.class);
    }

    @GET
    @Path("/getResponse/{message}")
    public String getResponse(@PathParam("message") String message, @QueryParam("test-port") int port) {
        Exchange exchange = producerTemplate.toF("netty-http:http://localhost:%d/response", port)
                .withBody(message)
                .send();
        FullHttpResponse response = exchange.getIn().getBody(NettyHttpMessage.class).getHttpResponse();
        String received = exchange.getIn().getBody(String.class);
        return received + ": " + response.status().reasonPhrase() + " " + response.status().code();
    }

    @GET
    @Path("/wildcard/{path}")
    public String wildcard(@PathParam("path") String path, @QueryParam("test-port") int port) {
        return producerTemplate.toF("netty-http:http://localhost:%d/%s", port, path)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .request(String.class);
    }

    @GET
    @Path("/consumer-proxy")
    public String proxy(@QueryParam("test-port") int port, @QueryParam("proxy-port") int proxyPort) {
        final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", proxyPort));
        final String url = "http://localhost:" + port + "/proxy";

        try {
            final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection(proxy);
            return new String(connection.getInputStream().readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/auth/{path}/{user}/{password}")
    public Response auth(@QueryParam("test-port") int port, @PathParam("path") String path, @PathParam("user") String user,
            @PathParam("password") String password) {
        final Exchange exchange = producerTemplate.toF("netty-http:http://localhost:%d/%s", port, path)
                .withHeaders(getAuthHeaders(user, password))
                .send();

        return Response.status(exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class)).build();
    }

    @GET
    @Path("/rest/{method}")
    public String rest(@QueryParam("rest-port") int port, @PathParam("method") String method) {
        return producerTemplate.toF("netty-http:http://localhost:%d/rest", port)
                .withHeader(Exchange.HTTP_METHOD, method)
                .request(String.class);
    }

    @GET
    @Path("/rest/pojo/{type}")
    public String restPojo(@QueryParam("rest-port") int port, @PathParam("type") String type) {
        final String body;
        final String contentType;
        if ("json".equals(type)) {
            body = "{\"firstName\":\"John\", \"lastName\":\"Doe\"}";
            contentType = "application/json";
        } else {
            body = "<user firstName=\"John\" lastName=\"Doe\"/>";
            contentType = "text/xml";
        }
        return producerTemplate.toF("netty-http:http://localhost:%d/rest/%s", port, type)
                .withBody(body)
                .withHeaders(Map.of(Exchange.HTTP_METHOD, "POST", "Content-Type", contentType))
                .request(String.class);
    }

    @GET
    @Path("/jaas/{user}/{password}")
    public Response auth(@QueryParam("test-port") int port, @PathParam("user") String user,
            @PathParam("password") String password) {
        final Exchange exchange = producerTemplate
                .toF("netty-http:http://localhost:%d/jaas", port)
                .withHeaders(getAuthHeaders(user, password))
                .send();

        return Response.status(exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class)).build();
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
