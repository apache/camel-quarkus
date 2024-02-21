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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.util.CollectionHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(WithRoutesOverridesTest.Resources.class)
public class WithRoutesOverridesTest {

    @ParameterizedTest
    @CsvSource({
            "r1,direct://r1override",
            "r2,direct://r2",
            "r3,direct://r3",
            "r4,direct://r4override",
            "r5,direct://r5override",
    })
    public void overrides(String id, String expected) {
        JsonPath p = given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/camel-k/inspect/route/" + id)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getString("id"))
                .isEqualTo(id);
        assertThat(p.getString("input"))
                .isEqualTo(expected);
    }

    public static class Resources implements QuarkusTestResourceLifecycleManager {
        @Override
        public Map<String, String> start() {
            return CollectionHelper.mapOf(
                    "camel.k.routes.overrides[0].input.from", "direct:r1",
                    "camel.k.routes.overrides[0].input.with", "direct:r1override",
                    "camel.k.routes.overrides[1].id", "r2invalid",
                    "camel.k.routes.overrides[1].input.from", "direct:r2",
                    "camel.k.routes.overrides[1].input.with", "direct:r2override",
                    "camel.k.routes.overrides[2].id", "r3",
                    "camel.k.routes.overrides[2].input.from", "direct:r3invalid",
                    "camel.k.routes.overrides[2].input.with", "direct:r3override",
                    "camel.k.routes.overrides[3].id", "r4",
                    "camel.k.routes.overrides[3].input.from", "direct:r4",
                    "camel.k.routes.overrides[3].input.with", "direct:r4override",
                    "camel.k.routes.overrides[4].input.with", "direct:r5invalid",
                    "camel.k.routes.overrides[5].id", "r5",
                    "camel.k.routes.overrides[5].input.with", "direct:r5override",
                    "camel.k.sources[0].location", "classpath:routes.yaml",
                    "camel.k.sources[0].type", "source");
        }

        @Override
        public void stop() {
        }
    }
}
