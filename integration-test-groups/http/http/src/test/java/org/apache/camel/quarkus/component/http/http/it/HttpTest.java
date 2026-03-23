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
package org.apache.camel.quarkus.component.http.http.it;

import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import org.apache.camel.quarkus.component.http.common.AbstractHttpTest;
import org.apache.camel.quarkus.component.http.common.HttpTestResource;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@TestCertificates(certificates = {
        @Certificate(name = HttpTestResource.KEYSTORE_NAME, formats = {
                Format.PKCS12 }, password = HttpTestResource.KEYSTORE_PASSWORD) })
@QuarkusTest
@QuarkusTestResource(HttpTestResource.class)
public class HttpTest extends AbstractHttpTest {
    @Override
    public String component() {
        return "http";
    }

    @Test
    public void basicAuthCache() {
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .when()
                .get("/test/client/{component}/auth/basic/cache", component())
                .then()
                .statusCode(200)
                .body(is("Component " + component() + " is using basic auth"));
    }

    @Test
    public void sendDynamic() {
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .accept(ContentType.JSON)
                .when()
                .get("/test/client/{component}/send-dynamic", component())
                .then()
                .statusCode(200)
                .body(
                        "q", is(not(empty())),
                        "fq", is(not(empty())));
    }

    @Test
    public void httpOperationFailedException() {
        RestAssured
                .given()
                .when()
                .get("/test/client/{component}/operation/failed/exception", component())
                .then()
                .statusCode(200)
                .body(is("Handled HttpOperationFailedException"));
    }

    @Override
    @Test
    public void compression() {
        RestAssured
                .when()
                .get("/test/client/{component}/compression", component())
                .then()
                .statusCode(200)
                .body(is("Compressed response"));
    }

    @ParameterizedTest
    @MethodSource("proxyProviders")
    void testNonProxyRouting(String nonProxyHosts, int proxyPort, String proxyHost, int status, String expectedBody,
            boolean proxyShouldBeInvoked) {
        int before = getProxyInvocations();
        var response = RestAssured.given()
                .queryParam("non-proxy-hosts", nonProxyHosts)
                .queryParam("proxy-port", proxyPort)
                .queryParam("proxy-host", proxyHost)
                .when()
                .get("/test/client/{component}/nonProxy", component())
                .then()
                .statusCode(status);

        response.body("metadata.groupId", is(expectedBody));

        int after = getProxyInvocations();

        if (proxyShouldBeInvoked) {
            assertTrue(after > before, "Proxy count should have increased. Before: " + before + ", after: " + after);
        } else {
            assertEquals(after, before, "Proxy invocation count should be the same.");
        }
    }

    static Stream<Arguments> proxyProviders(Config config) {
        String host = config.getValue("proxy.host", String.class);
        int actualPort = config.getValue("proxy.port", Integer.class);
        int fakePort = RestAssured.port;
        String expectedGroupId = "org.apache.camel.quarkus";

        return Stream.of(
                arguments("repo.maven.apache.org", actualPort, host, 200, expectedGroupId, false),
                arguments("*.apache.org", fakePort, host, 200, expectedGroupId, false),
                arguments("*localhost*", actualPort, host, 200, expectedGroupId, true));
    }

}
