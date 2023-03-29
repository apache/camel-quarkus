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
package org.apache.camel.quarkus.component.http.it;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.multipart.MultipartForm;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.Message;
import org.apache.camel.PropertyBindingException;
import org.apache.camel.ResolveEndpointFailedException;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.EndpointConsumerBuilder;
import org.apache.camel.builder.EndpointProducerBuilder;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.util.IOHelper;
import org.eclipse.microprofile.config.ConfigProvider;

import static org.apache.camel.component.vertx.http.VertxHttpConstants.CONTENT_TYPE_FORM_URLENCODED;

@Path("/test/client")
@ApplicationScoped
public class HttpResource {

    public static final String PROXIED_URL = "https://repo.maven.apache.org/maven2/org/apache/camel/quarkus/camel-quarkus-%s/maven-metadata.xml";
    public static final String USER_ADMIN = "admin";
    public static final String USER_ADMIN_PASSWORD = "adm1n";
    public static final String USER_NO_ADMIN = "noadmin";
    public static final String USER_NO_ADMIN_PASSWORD = "n0Adm1n";

    @Inject
    FluentProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    // *****************************
    //
    // camel-http
    //
    // *****************************

    @Path("/http/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String httpGet(@QueryParam("test-port") int port) {
        return producerTemplate
                .toF("http://localhost:%d/service/get?bridgeEndpoint=true&connectTimeout=2000", port)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .request(String.class);
    }

    @Path("/http/get-https")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String httpGetHttps(@QueryParam("test-port") int port) {
        return producerTemplate
                .toF("https://localhost:%d/countries/cz?bridgeEndpoint=true&sslContextParameters=#sslContextParameters", port)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .request(String.class);
    }

    @Path("/http/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String httpPost(@QueryParam("test-port") int port, String message) {
        return producerTemplate
                .toF("http://localhost:%d/service/toUpper?bridgeEndpoint=true", port)
                .withBody(message)
                .withHeader(Exchange.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .withHeader(Exchange.HTTP_METHOD, "POST")
                .request(String.class);
    }

    @Path("/http/auth/basic")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response httpBasicAuth(
            @QueryParam("test-port") int port,
            @QueryParam("username") String username,
            @QueryParam("password") String password) {

        Exchange result = producerTemplate
                .withHeader(Exchange.HTTP_QUERY, "component=http")
                .toF("http://localhost:%d/test/client/auth/basic?throwExceptionOnFailure=false"
                        + "&authMethod=BASIC"
                        + "&authUsername=%s"
                        + "&authPassword=%s", port, username, password)
                .send();

        Integer status = result.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String body = result.getMessage().getBody(String.class);
        return Response.status(status).entity(body).build();
    }

    @Path("/http/auth/basic/cache")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response httpBasicAuthCache(@QueryParam("test-port") int port) {

        Exchange result = producerTemplate
                .withHeader(Exchange.HTTP_QUERY, "component=http")
                .toF("http://localhost:%d/test/client/auth/basic"
                        + "?throwExceptionOnFailure=false"
                        + "&httpContext=#basicAuthContext", port)
                .send();

        Integer status = result.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String body = result.getMessage().getBody(String.class);
        return Response.status(status).entity(body).build();
    }

    @Path("/http/proxy")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public String httpProxy() {
        Integer proxyPort = ConfigProvider.getConfig().getValue("tiny.proxy.port", Integer.class);
        return producerTemplate
                .toF("%s?"
                        + "proxyAuthMethod=Basic"
                        + "&proxyAuthScheme=http"
                        + "&proxyAuthHost=localhost"
                        + "&proxyAuthPort=%d"
                        + "&proxyAuthUsername=%s"
                        + "&proxyAuthPassword=%s", String.format(PROXIED_URL, "http"), proxyPort, USER_ADMIN,
                        USER_ADMIN_PASSWORD)
                .request(String.class);
    }

    @Path("/http/compression")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String httpCompression(@QueryParam("test-port") int port) {
        return producerTemplate
                .toF("http://localhost:%d/compressed?bridgeEndpoint=true", port)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .withHeader("Accept-Encoding", "gzip, deflate")
                .request(String.class);
    }

    @Path("/http/operation/failed/exception")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String httpOperationFailedException() {
        producerTemplate.to("direct:httpOperationFailedException").send();
        return consumerTemplate.receiveBody("seda:dlq", 5000, String.class);
    }

    @Path("/http/serialized/exception")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String httpSerializedException(@QueryParam("test-port") int port) {
        Exchange exchange = producerTemplate
                .toF("http://localhost:%d/test/server/serialized/exception?bridgeEndpoint=true&transferException=true", port)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .send();
        return exchange.getException().getClass().getName();
    }

    // *****************************
    //
    // camel-netty-http
    //
    // *****************************

    @Path("/netty-http/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String nettyHttpGet(@QueryParam("test-port") int port) {
        return producerTemplate
                .toF("netty-http:http://localhost:%d/service/get?bridgeEndpoint=true", port)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .request(String.class);
    }

    @Path("/netty-http/get-https")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String nettyHttpGetHttps(@QueryParam("test-port") int port) {
        return producerTemplate
                .toF("netty-http:https://localhost:%d/countries/cz?sslContextParameters=#sslContextParameters", port)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .request(String.class);
    }

    @Path("/netty-http/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String nettyHttpPost(@QueryParam("test-port") int port, String message) {
        return producerTemplate
                .toF("netty-http:http://localhost:%d/service/toUpper?bridgeEndpoint=true", port)
                .withBody(message)
                .withHeader(Exchange.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .withHeader(Exchange.HTTP_METHOD, "POST")
                .request(String.class);
    }

    @Path("/netty-http/auth/basic")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response nettyHttpBasicAuth(
            @QueryParam("test-port") int port,
            @QueryParam("username") String username,
            @QueryParam("password") String password) {

        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        Exchange result = producerTemplate
                .withHeader(Exchange.HTTP_QUERY, "component=netty-http")
                .withHeader("Authorization", "Basic " + encoded)
                .toF("netty-http:http://localhost:%d/test/client/auth/basic?throwExceptionOnFailure=false", port)
                .send();

        Integer status = result.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String body = result.getMessage().getBody(String.class);
        return Response.status(status).entity(body).build();
    }

    @Path("/netty-http/proxy")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public String nettyHttpProxy() {
        String url = String.format(PROXIED_URL, "netty-http");
        return producerTemplate
                .withHeader(Exchange.HTTP_QUERY, "component=netty-http")
                .toF("netty-http:%s?clientInitializerFactory=#proxyCapableClientInitializerFactory", url)
                .request(String.class);
    }

    @Path("/netty-http/compression")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String nettyHttpCompression(@QueryParam("test-port") int port) throws IOException {
        byte[] compressed = producerTemplate
                .toF("netty-http:http://localhost:%d/compressed", port)
                .withHeader("Accept-Encoding", "gzip, deflate")
                .request(byte[].class);

        try (GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return IOHelper.loadText(inputStream).trim();
        }
    }

    @Path("/netty-http/serialized/exception")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String nettyHttpSerializedException(@QueryParam("test-port") int port) {
        Exchange exchange = producerTemplate
                .toF("netty-http:http://localhost:%d/test/server/serialized/exception?transferException=true", port)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .send();
        return exchange.getException().getClass().getName();
    }

    // *****************************
    //
    // camel-vertx-http
    //
    // *****************************

    @Path("/vertx-http/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String vertxHttpGet(@QueryParam("test-port") int port) {
        return producerTemplate
                .toF("vertx-http:http://localhost:%d/service/get", port)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .request(String.class);
    }

    @Path("/vertx-http/get-https")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String vertxHttpHttps(@QueryParam("test-port") int port) {
        return producerTemplate
                .toF("vertx-http:https://localhost:%d/countries/cz?sslContextParameters=#sslContextParameters", port)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .request(String.class);
    }

    @Path("/vertx-http/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String vertxHttpPost(@QueryParam("test-port") int port, String message) {
        return producerTemplate
                .toF("vertx-http:http://localhost:%d/service/toUpper", port)
                .withBody(message)
                .withHeader(Exchange.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .withHeader(Exchange.HTTP_METHOD, "POST")
                .request(String.class);
    }

    @Path("/vertx-http/auth/basic")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response vertxHttpBasicAuth(
            @QueryParam("test-port") int port,
            @QueryParam("username") String username,
            @QueryParam("password") String password) {

        Exchange result = producerTemplate
                .withHeader(Exchange.HTTP_QUERY, "component=vertx-http")
                .toF("vertx-http:http://localhost:%d/test/client/auth/basic?throwExceptionOnFailure=false"
                        + "&basicAuthUsername=%s"
                        + "&basicAuthPassword=%s", port, username, password)
                .send();

        Integer status = result.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String body = result.getMessage().getBody(String.class);
        return Response.status(status).entity(body).build();
    }

    @Path("/vertx-http/proxy")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public String vertxHttpProxy() {
        Integer proxyPort = ConfigProvider.getConfig().getValue("tiny.proxy.port", Integer.class);
        return producerTemplate
                .toF("vertx-http:%s?"
                        + "proxyHost=localhost"
                        + "&proxyPort=%d"
                        + "&proxyType=HTTP"
                        + "&proxyUsername=%s"
                        + "&proxyPassword=%s", String.format(PROXIED_URL, "vertx-http"), proxyPort, USER_ADMIN,
                        USER_ADMIN_PASSWORD)
                .request(String.class);
    }

    @Path("/vertx-http/compression")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String vertxHttpCompression(@QueryParam("test-port") int port) {
        return producerTemplate
                .toF("vertx-http:http://localhost:%d/compressed?useCompression=true", port)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .withHeader("Accept-Encoding", "gzip, deflate")
                .request(String.class);
    }

    @Path("/vertx-http/serialized/exception")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String vertxHttpSerializedException(@QueryParam("test-port") int port) {
        Exchange exchange = producerTemplate
                .toF("vertx-http:http://localhost:%d/test/server/serialized/exception?transferException=true", port)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .send();
        return exchange.getException().getClass().getName();
    }

    @Path("/vertx-http/multipart-form-params")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String vertxHttpMultipartFormParams(@QueryParam("test-port") int port,
            @QueryParam("organization") String organization,
            @QueryParam("project") String project) {
        return producerTemplate
                .toF("vertx-http:http://localhost:%d/service/multipart-form-params", port)
                .withBody("organization=" + organization + "&project=" + project)
                .withHeader(Exchange.CONTENT_TYPE, CONTENT_TYPE_FORM_URLENCODED)
                .request(String.class);
    }

    @Path("/vertx-http/multipart-form-data")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String vertxHttpMultipartFormData(@QueryParam("test-port") int port) {

        MultipartForm form = MultipartForm.create();
        form.binaryFileUpload("part-1", "test1.txt", Buffer.buffer("part1=content1".getBytes(StandardCharsets.UTF_8)),
                "text/plain");
        form.binaryFileUpload("part-2", "test2.xml",
                Buffer.buffer("<part2 value=\"content2\"/>".getBytes(StandardCharsets.UTF_8)), "text/xml");

        return producerTemplate
                .toF("vertx-http:http://localhost:%d/service/multipart-form-data", port)
                .withBody(form)
                .request(String.class);
    }

    @Path("/vertx-http/custom-vertx-options")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String vertxHttpCustomVertxOptions(@QueryParam("test-port") int port) {
        try {
            producerTemplate
                    .toF("vertx-http:http://localhost:%d/service/custom-vertx-options?vertxOptions=#myVertxOptions", port)
                    .request();
            return "NOT_EXPECTED: the custom vertxOptions should have triggered a ResolveEndpointFailedException";
        } catch (ResolveEndpointFailedException refex) {
            Throwable firstLevelExceptionCause = refex.getCause();
            if (firstLevelExceptionCause instanceof PropertyBindingException) {
                if (firstLevelExceptionCause.getCause() instanceof IllegalArgumentException) {
                    return "OK: the custom vertxOptions has triggered the expected exception";
                }
                return "NOT_EXPECTED: the 2nd level exception cause should be of type IllegalArgumentException";
            } else {
                return "NOT_EXPECTED: the 1st level exception cause should be of type PropertyBindingException";
            }
        }
    }

    @Path("/vertx-http/session-management")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String vertxHttpSessionManagement(@QueryParam("test-port") int port) {
        String vertxHttpBaseUri = "vertx-http:http://localhost:" + port + "/service/session-management";
        Exchange result = producerTemplate
                .toF("%s/secure?sessionManagement=true&cookieStore=#myVertxCookieStore", vertxHttpBaseUri)
                .request(Exchange.class);

        HttpOperationFailedException exception = result.getException(HttpOperationFailedException.class);
        if (exception.getStatusCode() != 403) {
            return "NOT_EXPECTED: The first request in the session is expected to return HTTP 403";
        }

        result = producerTemplate
                .toF("%s/login?sessionManagement=true&cookieStore=#myVertxCookieStore", vertxHttpBaseUri)
                .withHeader("username", "my-username")
                .withHeader("password", "my-password")
                .request(Exchange.class);

        Message msg = result.getMessage();
        if (msg == null) {
            return "NOT_EXPECTED: The second request in the session should return a message";
        } else {
            String setCookieHeader = msg.getHeader("Set-Cookie", String.class);
            if (setCookieHeader == null || !setCookieHeader.contains("sessionId=my-session-id-123")) {
                return "NOT_EXPECTED: The message should have a Set-Cookie String header containing \"sessionId=my-session-id-123\"";
            }
        }

        return producerTemplate
                .toF("%s/secure?sessionManagement=true&cookieStore=#myVertxCookieStore", vertxHttpBaseUri)
                .request(String.class);
    }

    @Path("/vertx-http/buffer-conversion-with-charset")
    @GET
    public byte[] vertxBufferConversionWithCharset(@QueryParam("string") String string, @QueryParam("charset") String charset) {
        Buffer buffer = producerTemplate
                .to("direct:vertx-http-buffer-conversion-with-charset")
                .withBody(string)
                .withHeader(Exchange.CONTENT_TYPE, "text/plain;charset=" + charset)
                .request(Buffer.class);

        return buffer.getBytes();
    }

    // *****************************
    //
    // Send dynamic tests
    //
    // *****************************

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

    @Path("/auth/basic")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public String basicAuth(@QueryParam("component") String component) {
        return "Component " + component + " is using basic auth";
    }

    @ApplicationScoped
    RoutesBuilder sendDynamicRoutes() {
        return new EndpointRouteBuilder() {
            @Override
            public void configure() throws Exception {
                final EndpointConsumerBuilder trigger = direct(
                        "send-dynamic");
                final EndpointProducerBuilder service = http(
                        "localhost:${header.SendDynamicHttpEndpointPort}/test/client/send-dynamic/service?q=*&fq=publication_date:%5B${date:now-72h:yyyy-MM-dd}T00:00:00Z%20TO%20${date:now-24h:yyyy-MM-dd}T23:59:59Z%5D&wt=xml&indent=false&start=0&rows=100");

                from(trigger)
                        .setHeader(Exchange.HTTP_METHOD).constant("GET")
                        .toD(service)
                        .convertBodyTo(String.class);
            }
        };
    }
}
