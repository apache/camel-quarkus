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
package org.apache.camel.quarkus.main;

import java.net.HttpURLConnection;
import javax.ws.rs.core.MediaType;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.camel.quarkus.main.support.SupportListener;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@QuarkusTest
public class CamelTest {
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
    public void testSetCamelContextName() {
        Response response = RestAssured.get("/test/context/name").andReturn();

        assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        assertNotEquals("my-ctx-name", response.body().asString());

        RestAssured.given()
            .contentType(ContentType.TEXT).body("my-ctx-name")
            .post("/test/context/name")
            .then().body(is("my-ctx-name"));
    }

    @Test
    public void testMainInstance() {
        JsonPath p = RestAssured.given()
            .accept(MediaType.APPLICATION_JSON)
            .get("/test/main/describe")
            .then()
                .statusCode(200)
            .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("listeners", String.class))
            .containsOnly(CamelMainEventDispatcher.class.getName(), SupportListener.class.getName());
        assertThat(p.getList("routeBuilders", String.class))
            .containsOnly(CamelRoute.class.getName());
        assertThat(p.getList("routes", String.class))
            .hasSize(2)
            .containsOnly("keep-alive", "listener");

        assertThat(p.getBoolean("autoConfigurationLogSummary")).isFalse();

    }
}
