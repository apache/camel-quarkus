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
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;

class DiagramTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(DiagramRoutes.class)
                    .addAsResource(new StringAsset("quarkus.camel.console.enabled=true\n"), "application.properties"));

    @Test
    void routeDiagramHtml() {
        RestAssured.given()
                .accept("text/html")
                .queryParam("format", "html")
                .queryParam("mode", "route")
                .when()
                .get("/q/camel/diagram/route-diagram")
                .then()
                .statusCode(200)
                .contentType("text/html")
                .body(containsString("<html"),
                        containsString("camel-route-diagram"));
    }

    @Test
    void routeStructureJson() {
        RestAssured.given()
                .accept("application/json")
                .when()
                .get("/q/camel/diagram/route-structure")
                .then()
                .statusCode(200)
                .contentType("application/json")
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
                .body(containsString("nodes"),
                        not(emptyString()));
    }

    @Test
    void nonExistentConsole() {
        RestAssured.get("/q/camel/diagram/nonexistent")
                .then()
                .statusCode(404);
    }
}
