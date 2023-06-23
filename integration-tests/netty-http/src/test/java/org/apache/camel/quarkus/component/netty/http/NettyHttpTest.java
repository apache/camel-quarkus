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

import java.util.List;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(NettyHttpTestResource.class)
public class NettyHttpTest {
    @Test
    public void testExtractHttpRequestFromNettyHttpMessage() {
        final String method = "POST";
        final String headerName = "testHeaderKey";
        final String headerValue = "testHeaderValue";
        final String body = "Test body";

        final Response response = RestAssured
                .when()
                .get("/netty/http/getRequest/{method}/{hName}/{hValue}/{body}", method, headerName, headerValue, body);
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody().print().split(",")).containsAll(List.of(method, body, headerName + ":" + headerValue));
    }

    @Test
    public void testExtractHttpResponseFromNettyHttpMessage() {
        final String message = "httpResponseTest";
        RestAssured
                .when()
                .get("/netty/http/getResponse/{message}", message)
                .then()
                .statusCode(200)
                .body(is("Received message " + message + ": OK 200"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "wildcard", "wildcard/example.txt" })
    public void testWildcardMatching(String path) {
        RestAssured
                .when()
                .get("/netty/http/wildcard/{path}", path)
                .then()
                .statusCode(200)
                .body(is("wildcard matched"));
    }

    @Test
    public void testProxy() {
        RestAssured
                .when()
                .get("/netty/http/proxy")
                .then()
                .statusCode(200)
                .body(is("proxy"));
    }
}
