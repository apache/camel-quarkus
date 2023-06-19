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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(NettyHttpTestResource.class)
public class NettyHttpRestTest {
    @ParameterizedTest
    @ValueSource(strings = { "GET", "POST", "PUT", "DELETE" })
    public void testRest(String method) {
        final ValidatableResponse response = RestAssured
                .when()
                .get("/netty/http/rest/{method}", method)
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
                .when()
                .get("/netty/http/rest/pojo/{type}", type)
                .then()
                .statusCode(200)
                .body(is("Received: John Doe"));
    }
}
