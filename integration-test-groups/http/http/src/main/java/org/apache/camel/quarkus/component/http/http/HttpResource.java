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
package org.apache.camel.quarkus.component.http.http;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.EndpointConsumerBuilder;
import org.apache.camel.builder.EndpointProducerBuilder;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.quarkus.component.http.common.AbstractHttpResource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@Path("/test/client/http")
@ApplicationScoped
public class HttpResource extends AbstractHttpResource {
    @Override
    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String httpGet(@QueryParam("test-port") int port) {
        return producerTemplate
                .toF("http://localhost:%d/service/common/get?bridgeEndpoint=true&connectTimeout=2000", port)
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
                .toF("http://localhost:%d/service/common/toUpper?bridgeEndpoint=true", port)
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
        Exchange result = producerTemplate
                .withHeader(Exchange.HTTP_QUERY, "component=http")
                .toF("http://localhost:%d/test/client/http/auth/basic/secured?throwExceptionOnFailure=false"
                        + "&authMethod=BASIC"
                        + "&authUsername=%s"
                        + "&authPassword=%s", port, username, password)
                .send();

        Integer status = result.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String body = result.getMessage().getBody(String.class);
        return Response.status(status).entity(body).build();
    }

    @Path("/auth/basic/cache")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response httpBasicAuthCache(@QueryParam("test-port") int port) {

        Exchange result = producerTemplate
                .withHeader(Exchange.HTTP_QUERY, "component=http")
                .toF("http://localhost:%d/test/client/http/auth/basic/secured"
                        + "?throwExceptionOnFailure=false"
                        + "&httpContext=#basicAuthContext", port)
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
        Config config = ConfigProvider.getConfig();
        String proxyHost = config.getValue("tiny.proxy.host", String.class);
        Integer proxyPort = config.getValue("tiny.proxy.port", Integer.class);
        return producerTemplate
                .toF("%s?"
                        + "proxyAuthMethod=Basic"
                        + "&proxyAuthScheme=http"
                        + "&proxyAuthHost=%s"
                        + "&proxyAuthPort=%d"
                        + "&proxyAuthUsername=%s"
                        + "&proxyAuthPassword=%s", String.format(PROXIED_URL, "http"), proxyHost, proxyPort, USER_ADMIN,
                        USER_ADMIN_PASSWORD)
                .request(String.class);
    }

    @Path("/send-dynamic")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getSendDynamic(@QueryParam("test-port") int port) {
        return producerTemplate
                .withHeader("SendDynamicHttpEndpointPort", port)
                .to("direct:send-dynamic")
                .request(String.class);
    }

    @Path("/send-dynamic/service")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject get(@QueryParam("q") String q, @QueryParam("fq") String fq) {
        return Json.createObjectBuilder()
                .add("q", q)
                .add("fq", fq)
                .build();
    }

    @ApplicationScoped
    RoutesBuilder sendDynamicRoutes() {
        return new EndpointRouteBuilder() {
            @Override
            public void configure() throws Exception {
                final EndpointConsumerBuilder trigger = direct(
                        "send-dynamic");
                final EndpointProducerBuilder service = http(
                        "localhost:${header.SendDynamicHttpEndpointPort}/test/client/http/send-dynamic/service?q=*&fq=publication_date:%5B${date:now-72h"
                                + ":yyyy-MM-dd}T00:00:00Z%20TO%20${date:now-24h:yyyy-MM-dd}T23:59:59Z%5D&wt=xml&indent=false&start=0&rows=100");

                from(trigger)
                        .setHeader(Exchange.HTTP_METHOD).constant("GET")
                        .toD(service)
                        .convertBodyTo(String.class);
            }
        };
    }

    @Path("/operation/failed/exception")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String operationFailedException() {
        producerTemplate.to("direct:httpOperationFailedException").send();
        return consumerTemplate.receiveBody("seda:dlq", 5000, String.class);
    }

    @Path("/compression")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String compression() {
        return producerTemplate
                .toF("http://localhost:%d/service/common/compress",
                        ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class))
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .withHeader("Accept-Encoding", "gzip, deflate")
                .request(String.class);
    }
}
