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
package org.apache.camel.quarkus.component.langchain4j.ingest.it;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The {@code docling} parser: a PDF dropped into the watched directory is converted to markdown
 * by docling-serve — stubbed with WireMock by default, see {@link IngestDoclingTestResource} —
 * and the markdown is ingested. The checked-in fixture carries the same sentence the stub
 * returns, so the test also holds against a real docling-serve
 * ({@code -Dcamel.quarkus.start-mock-backend=false}).
 */
@QuarkusTest
@QuarkusTestResource(value = IngestDoclingTestResource.class, restrictToAnnotatedClass = true)
class Langchain4jIngestDoclingTest {

    @Test
    void documentIsConvertedAndIngested() throws Exception {
        RestAssured.given().contentType(ContentType.BINARY)
                .body(getClass().getResourceAsStream("/valve.pdf").readAllBytes())
                .post("/langchain4j-ingest/binary/scans/valve.pdf")
                .then().statusCode(204);

        Awaitility.await().atMost(60, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Map<String, String> hit = Langchain4jIngestTest.hit(
                            "At what pressure does the valve seal?", "scans", "EPSILON-2");
                    assertNotNull(hit, "the document must be converted and its markdown ingested");
                    assertEquals("scans", hit.get("pipeline"));
                    assertEquals("valve.pdf", hit.get("documentId"));
                });
    }
}
