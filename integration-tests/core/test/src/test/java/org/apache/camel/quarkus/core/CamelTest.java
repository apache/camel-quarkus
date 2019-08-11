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
package org.apache.camel.quarkus.core;

import java.net.HttpURLConnection;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class CamelTest {

    @Test
    public void testRoutes() {
        RestAssured.when().get("/test/routes").then().body(containsString("timer"));
    }

    @Test
    public void testProperties() {
        RestAssured.when().get("/test/property/camel.context.name").then().body(is("quarkus-camel-example"));
        RestAssured.when().get("/test/property/camel.component.timer.basic-property-binding").then().body(is("true"));
    }

    @Test
    public void timerPropertyPropagated() {
        RestAssured.when().get("/test/timer/property-binding").then().body(is("true"));
    }

    @Test
    public void testRegistry() {
        RestAssured.when().get("/test/registry/produces-config-build").then().body(is("true"));
        RestAssured.when().get("/test/registry/produces-config-runtime").then().body(is("true"));
    }

    @Test
    public void testRegistryBuildItem() {
        Response response = RestAssured.get("/test/registry/log/exchange-formatter").andReturn();

        assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        assertTrue(response.jsonPath().getBoolean("show-all"));
        assertTrue(response.jsonPath().getBoolean("multi-line"));
    }
}
