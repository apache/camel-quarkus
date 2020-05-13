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
package org.apache.camel.quarkus.component.opentracing.it;

import java.util.List;
import java.util.Map;

import io.opentracing.tag.Tags;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class OpenTracingTest {

    @BeforeAll
    public static void setUp() {
        Assumptions.assumeFalse(ConfigProvider.getConfig().getOptionalValue(
                "quarkus.camel.main.lightweight", Boolean.class).orElse(false),
                "TODO: Disabled in lightweight mode");
    }

    @Test
    public void testTraceRoute() {
        // Generate messages
        for (int i = 0; i < 5; i++) {
            RestAssured.get("/opentracing/test/trace")
                    .then()
                    .statusCode(200);

            // No spans should be recorded for this route as they are excluded by camel.opentracing.exclude-patterns in
            // application.properties
            RestAssured.get("/opentracing/test/trace/filtered")
                    .then()
                    .statusCode(200);
        }

        // Retrieve recorded spans
        JsonPath jsonPath = RestAssured.given()
                .get("/opentracing/spans")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        List<Map<String, String>> spans = jsonPath.get();
        assertEquals(5, spans.size());

        for (Map<String, String> span : spans) {
            assertEquals("server", span.get(Tags.SPAN_KIND.getKey()));
            assertEquals("camel-platform-http", span.get(Tags.COMPONENT.getKey()));
            assertEquals("200", span.get(Tags.HTTP_STATUS.getKey()));
            assertEquals("GET", span.get(Tags.HTTP_METHOD.getKey()));
            assertEquals("platform-http:///opentracing/test/trace?httpMethodRestrict=GET", span.get("camel.uri"));
            assertTrue(span.get(Tags.HTTP_URL.getKey()).endsWith("/opentracing/test/trace"));
        }
    }
}
