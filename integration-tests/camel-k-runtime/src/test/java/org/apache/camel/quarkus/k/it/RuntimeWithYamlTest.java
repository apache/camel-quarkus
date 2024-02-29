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
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.quarkus.k.it.yaml.MyProcessor;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.apache.camel.util.CollectionHelper.mapOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(RuntimeWithYamlTest.Profile.class)
public class RuntimeWithYamlTest {
    @Test
    public void inspect() {
        given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/camel-k/inspect")
                .then()
                .statusCode(200)
                .body("route-definitions", hasItem("my-yaml-route"));

        given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/camel-k/registry/beans/my-bean")
                .then()
                .statusCode(200)
                .body("name", is("my-bean-name"));

        given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/camel-k/registry/beans/myProcessor")
                .then()
                .statusCode(200);
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return mapOf(
                    // sources
                    "camel.main.routes-include-pattern", "classpath:routes/routes-with-beans.yaml",
                    // misc
                    "camel.beans.myProcessor", "#class:" + MyProcessor.class.getName());
        }
    }
}
