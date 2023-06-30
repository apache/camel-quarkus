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
package org.apache.camel.quarkus.component.http.netty.it;

import java.util.List;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.camel.quarkus.component.http.common.AbstractHttpTest;
import org.apache.camel.quarkus.component.http.common.HttpTestResource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(HttpTestResource.class)
@QuarkusTestResource(NettyHttpTestResource.class)
public class NettyHttpTest extends AbstractHttpTest {
    @Override
    public String component() {
        return "netty-http";
    }

    @Test
    public void basicNettyHttpServer() {
        RestAssured
                .given()
                .port(ConfigProvider.getConfig().getValue("camel.netty-http.test-port", Integer.class))
                .when()
                .get("/test/server/hello")
                .then()
                .statusCode(200)
                .body(is("Netty Hello World"));
    }

    @Test
    public void transferException() {
        RestAssured
                .given()
                .queryParam("test-port", getPort("camel.netty-http.test-port"))
                .when()
                .get("/test/client/{component}/serialized/exception", component())
                .then()
                .statusCode(200)
                .body(is("java.lang.IllegalStateException"));
    }

    @Override
    @Test
    public void compression() {
        final int port = getPort("camel.netty-http.compression-test-port");
        RestAssured
                .given()
                .queryParam("test-port", port)
                .when()
                .get("/test/client/{component}/compression", component())
                .then()
                .statusCode(200)
                .body(is("Netty Hello World Compressed"));
    }

    @Test
    public void testExtractHttpRequestFromNettyHttpMessage() {
        final String method = "POST";
        final String headerName = "testHeaderKey";
        final String headerValue = "testHeaderValue";
        final String body = "Test body";

        final Response response = RestAssured
                .given()
                .queryParam("test-port", getPort("camel.netty-http.port"))
                .when()
                .get("/test/client/{component}/getRequest/{method}/{hName}/{hValue}/{body}", component(), method, headerName,
                        headerValue, body);
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody().print().split(",")).containsAll(List.of(method, body, headerName + ":" + headerValue));
    }

    @Test
    public void testExtractHttpResponseFromNettyHttpMessage() {
        final String message = "httpResponseTest";
        RestAssured
                .given()
                .queryParam("test-port", getPort("camel.netty-http.port"))
                .when()
                .get("/test/client/{component}/getResponse/{message}", component(), message)
                .then()
                .statusCode(200)
                .body(is("Received message " + message + ": OK 200"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "wildcard", "wildcard/example.txt" })
    public void testWildcardMatching(String path) {
        RestAssured
                .given()
                .queryParam("test-port", getPort("camel.netty-http.port"))
                .when()
                .get("/test/client/{component}/wildcard/{path}", component(), path)
                .then()
                .statusCode(200)
                .body(is("wildcard matched"));
    }

    @Test
    public void testProxy() {
        RestAssured
                .given()
                .queryParam("test-port", getPort("camel.netty-http.port"))
                .queryParam("proxy-port", getPort("camel.netty-http.proxyPort"))
                .when()
                .get("/test/client/{component}/consumer-proxy", component())
                .then()
                .statusCode(200)
                .body(is("proxy"));
    }

    @ParameterizedTest
    @CsvSource({
            "null,null,401",
            "admin,wrongpass,401",
            "admin,adminpass,200"
    })
    public void testCredentials(String user, String password, int responseCode) {
        RestAssured
                .given()
                .queryParam("test-port", getPort("camel.netty-http.port"))
                .when()
                .get("/test/client/{component}/auth/{path}/{user}/{password}", component(),
                        "auth?securityConfiguration=#securityConfig", user, password)
                .then()
                .statusCode(responseCode);
    }

    @ParameterizedTest
    @CsvSource({
            "admin,admin,adminpass,200",
            "admin,guest,guestpass,401",
            "admin,null,null,401",
            "guest,admin,adminpass,200",
            "guest,guest,guestpass,200",
            "guest,null,null,401",
            "wildcard,admin,adminpass,200",
            "wildcard,guest,guestpass,200",
            "wildcard,null,null,401",
            "public,admin,adminpass,200",
            "public,guest,guestpass,200",
            "public,null,null,200",
    })
    public void testAcls(String endpoint, String user, String password, int responseCode) {
        RestAssured
                .given()
                .queryParam("test-port", getPort("camel.netty-http.port"))
                .when()
                .get("/test/client/{component}/auth/{path}/{user}/{password}", component(),
                        "acls/" + endpoint + "?securityConfiguration=#acl" + endpoint,
                        user, password)
                .then()
                .statusCode(responseCode);
    }

    @ParameterizedTest
    @ValueSource(strings = { "GET", "POST", "PUT", "DELETE" })
    public void testRest(String method) {
        final ValidatableResponse response = RestAssured
                .given()
                .queryParam("rest-port", getPort("camel.netty-http.restPort"))
                .when()
                .get("/test/client/{component}/rest/{method}", component(), method)
                .then();
        // DELETE is not defined in the routes, so the request should fail
        if ("DELETE".equals(method)) {
            response.statusCode(500);
        } else {
            response
                    .statusCode(200)
                    .body(is(method));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "json", "xml" })
    public void pojoTest(String type) {
        RestAssured
                .given()
                .queryParam("rest-port", getPort("camel.netty-http.restPort"))
                .when()
                .get("/test/client/{component}/rest/pojo/{type}", component(), type)
                .then()
                .statusCode(200)
                .body(is("Received: John Doe"));
    }
}
