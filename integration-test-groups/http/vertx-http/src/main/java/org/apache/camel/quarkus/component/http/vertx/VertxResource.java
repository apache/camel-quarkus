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
package org.apache.camel.quarkus.component.http.vertx;

import java.nio.charset.StandardCharsets;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.multipart.MultipartForm;
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
import org.apache.camel.Message;
import org.apache.camel.PropertyBindingException;
import org.apache.camel.ResolveEndpointFailedException;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.quarkus.component.http.common.AbstractHttpResource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import static org.apache.camel.component.vertx.http.VertxHttpConstants.CONTENT_TYPE_FORM_URLENCODED;

@Path("/test/client/vertx-http")
@ApplicationScoped
public class VertxResource extends AbstractHttpResource {

    @Override
    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String httpGet(@QueryParam("test-port") int port) {
        return producerTemplate
                .toF("vertx-http:http://localhost:%d/service/common/get", port)
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
                .toF("vertx-http:http://localhost:%d/service/common/toUpper", port)
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
                .withHeader(Exchange.HTTP_QUERY, "component=vertx-http")
                .toF("vertx-http:http://localhost:%d/test/client/vertx-http/auth/basic/secured?throwExceptionOnFailure=false"
                        + "&basicAuthUsername=%s"
                        + "&basicAuthPassword=%s", port, username, password)
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
                .toF("vertx-http:%s?"
                        + "proxyHost=%s"
                        + "&proxyPort=%d"
                        + "&proxyType=HTTP"
                        + "&proxyUsername=%s"
                        + "&proxyPassword=%s", String.format(PROXIED_URL, "vertx-http"), proxyHost, proxyPort, USER_ADMIN,
                        USER_ADMIN_PASSWORD)
                .request(String.class);
    }

    @Path("/compression")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String compression() {
        return producerTemplate
                .toF("vertx-http:http://localhost:%d/service/common/compress?useCompression=true",
                        ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class))
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .withHeader("Accept-Encoding", "gzip, deflate")
                .request(String.class);
    }

    @Path("/serialized/exception")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String vertxHttpSerializedException(@QueryParam("test-port") int port) {
        Exchange exchange = producerTemplate
                .toF("vertx-http:http://localhost:%d/test/server/serialized/exception?transferException=true", port)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .send();
        return exchange.getException().getClass().getName();
    }

    @Path("/multipart-form-params")
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

    @Path("/multipart-form-data")
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

    @Path("/custom-vertx-options")
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

    @Path("/session-management")
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

    @Path("/buffer-conversion-with-charset")
    @GET
    public byte[] vertxBufferConversionWithCharset(@QueryParam("string") String string, @QueryParam("charset") String charset) {
        Buffer buffer = producerTemplate
                .to("direct:vertx-http-buffer-conversion-with-charset")
                .withBody(string)
                .withHeader(Exchange.CONTENT_TYPE, "text/plain;charset=" + charset)
                .request(Buffer.class);

        return buffer.getBytes();
    }
}
