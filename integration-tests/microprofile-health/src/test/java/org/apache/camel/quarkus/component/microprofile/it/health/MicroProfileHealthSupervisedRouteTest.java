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
package org.apache.camel.quarkus.component.microprofile.it.health;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(SupervisedRouteTestProfile.class)
class MicroProfileHealthSupervisedRouteTest {
    @Test
    void supervisedRouteTest() throws InterruptedException {
        // Verify the initial health state
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            RestAssured.when().get("/q/health").then()
                    .contentType(ContentType.JSON)
                    .header("Content-Type", containsString("charset=UTF-8"))
                    .body("status", is("DOWN"),
                            "checks.status.findAll().unique()", contains("UP", "DOWN"),
                            "checks.find { it.name == 'camel-routes' }.status", is("DOWN"),
                            "checks.find { it.name == 'camel-consumers' }.status", is("DOWN"));
        });

        // camel.routecontroller.unhealthy-on-exhausted is false so the heath status should be UP
        await().pollDelay(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            RestAssured.when().get("/q/health").then()
                    .contentType(ContentType.JSON)
                    .header("Content-Type", containsString("charset=UTF-8"))
                    .body("status", is("UP"),
                            "checks.status.findAll().unique()", contains("UP"),
                            "checks.find { it.name == 'camel-routes' }.status", is("UP"),
                            "checks.find { it.name == 'camel-consumers' }.status", is("UP"));
        });
    }
}
