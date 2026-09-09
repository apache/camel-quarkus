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
package org.apache.camel.quarkus.component.diagram.it;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;

class DiagramTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(DiagramRoutes.class));

    @Test
    void routeStructureJson() {
        RestAssured.given()
                .accept("application/json")
                .when()
                .get("/q/camel/diagram/route-structure")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("X-Content-Type-Options", "nosniff")
                .body(containsString("routes"),
                        containsString("diagram-test-route"));
    }

    @Test
    void routeTopologyJson() {
        RestAssured.given()
                .accept("application/json")
                .when()
                .get("/q/camel/diagram/route-topology")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("X-Content-Type-Options", "nosniff")
                .body(containsString("nodes"),
                        not(emptyString()));
    }

    @Test
    void allowedConsoleOptionsAreForwarded() {
        // The Dev UI web components pass filter & metric to route-structure, and external & metric to route-topology
        RestAssured.given()
                .accept("application/json")
                .queryParam("filter", "diagram-test-route")
                .queryParam("metric", "true")
                .when()
                .get("/q/camel/diagram/route-structure")
                .then()
                .statusCode(200)
                .body(containsString("diagram-test-route"));

        RestAssured.given()
                .accept("application/json")
                .queryParam("filter", "no-such-route")
                .when()
                .get("/q/camel/diagram/route-structure")
                .then()
                .statusCode(200)
                .body(not(containsString("diagram-test-route")));

        RestAssured.given()
                .accept("application/json")
                .queryParam("external", "true")
                .queryParam("metric", "true")
                .when()
                .get("/q/camel/diagram/route-topology")
                .then()
                .statusCode(200)
                .body(containsString("nodes"));
    }

    @Test
    void disallowedConsoleOptionsAreStripped() {
        // The filter is applied when it reaches the console, so the route being listed shows that it did not
        RestAssured.given()
                .accept("application/json")
                .queryParam("filter", "<script>alert(1)</script>")
                .queryParam("limit", "1")
                .when()
                .get("/q/camel/diagram/route-structure")
                .then()
                .statusCode(200)
                .body(containsString("diagram-test-route"),
                        not(containsString("<script")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "text/html",
            // What a browser sends when the URL is opened as a top level navigation
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
            "*/*" })
    void onlyJsonIsAcceptable(String accept) {
        // Console text output is markup meant for a browser to run, and this route serves JSON only
        RestAssured.given()
                .accept(accept)
                .when()
                .get("/q/camel/diagram/route-structure")
                .then()
                .statusCode(406)
                .body(emptyString());
    }

    @Test
    void nonExistentConsole() {
        RestAssured.get("/q/camel/diagram/nonexistent")
                .then()
                .statusCode(404);
    }

    @ParameterizedTest
    @ValueSource(strings = { "route-diagram", "context", "jvm", "health", "java-security" })
    void nonDiagramConsoleIsNotReachable(String consoleId) {
        // These consoles are registered and were previously rendered by this route, which exists to serve the two
        // consoles the Dev UI diagram page reads
        RestAssured.get("/q/camel/diagram/" + consoleId)
                .then()
                .statusCode(404);
    }
}
