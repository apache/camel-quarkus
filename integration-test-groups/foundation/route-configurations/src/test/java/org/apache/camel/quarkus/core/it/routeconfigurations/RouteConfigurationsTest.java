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
package org.apache.camel.quarkus.core.it.routeconfigurations;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
public class RouteConfigurationsTest {

    @Test
    public void sendNominalContentToRouteConfigurationWithExplicitIdShouldNotTriggerOnException() {
        String expected = "onException has NOT been triggered in routeConfigurationWithExplicitId";
        RestAssured.given()
                .body("explicit-nominal")
                .when()
                .get("/core/route-configurations/routeConfigurationWithExplicitId")
                .then()
                .body(is(expected));
    }

    @Test
    public void sendExceptionContentToRouteConfigurationWithExplicitIdShouldTriggerOnException() {
        String expected = "onException has been triggered in routeConfigurationWithExplicitId";
        RestAssured.given()
                .body("explicit-exception")
                .when()
                .get("/core/route-configurations/routeConfigurationWithExplicitId")
                .then()
                .body(is(expected));
    }

    @Test
    public void sendNominalContentToDefaultRouteConfigurationShouldNotTriggerOnException() {
        String expected = "onException has NOT been triggered in fallbackRouteConfiguration";
        RestAssured.given()
                .body("default-nominal")
                .when()
                .get("/core/route-configurations/fallbackRouteConfiguration")
                .then()
                .body(is(expected));
    }

    @Test
    public void sendExceptionContentToDefaultRouteConfigurationShouldTriggerOnException() {
        String expected = "onException has been triggered in fallbackRouteConfiguration";
        RestAssured.given()
                .body("default-exception")
                .when()
                .get("/core/route-configurations/fallbackRouteConfiguration")
                .then()
                .body(is(expected));
    }

    @Test
    public void sendContentToYamlRouteShouldTriggerOnExceptionInXmlRouteConfiguration() {
        String expected = "onException has been triggered in xmlRouteConfiguration";
        RestAssured.given()
                .get("/core/route-configurations/yamlRoute")
                .then()
                .body(is(expected));
    }

    @Test
    public void sendContentToXmlRouteShouldTriggerOnExceptionInYamlRouteConfiguration() {
        String expected = "onException has been triggered in yamlRouteConfiguration";
        RestAssured.given()
                .get("/core/route-configurations/xmlRoute")
                .then()
                .body(is(expected));
    }
}
