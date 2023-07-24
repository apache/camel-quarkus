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

import io.quarkus.test.junit.QuarkusTestProfile;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.apache.camel.util.CollectionHelper.mapOf;
import static org.assertj.core.api.Assertions.assertThat;

@Disabled("https://github.com/apache/camel-quarkus/issues/5235")
//@TestProfile(RuntimeCustomizerFallbackTest.Profile.class)
//@QuarkusTest
public class RuntimeCustomizerFallbackTest {
    @Test
    public void testContextCustomizerFromPropertiesFallback() {
        JsonPath p = given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/camel-k/inspect/context")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getBoolean("message-history")).isFalse();
        assertThat(p.getBoolean("load-type-converters")).isFalse();
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return mapOf(
                    "customizer.test.enabled", "true",
                    "customizer.test.message-history", "false");
        }
    }
}
