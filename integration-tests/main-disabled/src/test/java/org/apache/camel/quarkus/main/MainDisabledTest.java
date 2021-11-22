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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class MainDisabledTest {
    @Test
    public void inspect() {
        JsonPath path = RestAssured.when()
                .get("/main-disabled/inspect")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        assertThat(path.getList("routes", String.class)).hasSize(2);
    }

    @Test
    public void invokeMainDisabledRouteWithNominalBodyShouldNotTriggerRouteConfiguration() {
        RestAssured.given()
                .body("nominal")
                .when()
                .get("/main-disabled/invoke-main-disabled-route")
                .then()
                .statusCode(200)
                .body(is("onException has NOT been triggered in main-disabled-route-configuration"));
    }

    @Test
    public void invokeMainDisabledRouteWithExceptionBodyShouldTriggerRouteConfiguration() {
        RestAssured.given()
                .body("main-disabled-exception")
                .when()
                .get("/main-disabled/invoke-main-disabled-route")
                .then()
                .statusCode(200)
                .body(is("onException has been triggered in main-disabled-route-configuration"));
    }

    @Test
    public void invokeMainDisabledRouteCdiWithNominalBodyShouldNotTriggerRouteConfigurationCdi() {
        RestAssured.given()
                .body("nominal")
                .when()
                .get("/main-disabled/invoke-main-disabled-route-cdi")
                .then()
                .statusCode(200)
                .body(is("onException has NOT been triggered in main-disabled-route-configuration-cdi"));
    }

    @Test
    public void invokeMainDisabledRouteCdiWithExceptionBodyShouldTriggerRouteConfigurationCdi() {
        RestAssured.given()
                .body("main-disabled-cdi-exception")
                .when()
                .get("/main-disabled/invoke-main-disabled-route-cdi")
                .then()
                .statusCode(200)
                .body(is("onException has been triggered in main-disabled-route-configuration-cdi"));
    }

}
