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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
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
}
