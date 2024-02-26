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
package org.apache.camel.quarkus.k.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(RuntimeWithXmlTest.Profile.class)
public class RuntimeWithXmlTest {
    @Test
    public void inspect() {
        JsonPath p = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/camel-k/inspect")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("route-definitions", String.class))
                .contains("xml-simple-route", "xml-route-with-expression", "xml-greetings");
        assertThat(p.getList("rest-definitions", String.class))
                .contains("xml-rest-greetings");
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {

            // Return the configuration
            return Map.of(
                    // sources
                    "camel.main.routes-include-pattern", String.join(
                            ",",
                            "classpath:routes/routes.xml",
                            "classpath:routes/rests.xml",
                            "classpath:routes/routes-with-expression.xml"),
                    // misc
                    "the.body", "10");
        }

    }
}
