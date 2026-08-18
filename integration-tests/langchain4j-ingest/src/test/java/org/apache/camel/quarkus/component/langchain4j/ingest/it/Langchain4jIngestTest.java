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

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Configuration alone turns a directory into a knowledge base: a file dropped into the watched
 * directory is split, embedded and retrievable, with no route and no ingestion code written by
 * the application.
 */
@QuarkusTest
class Langchain4jIngestTest {

    @Test
    void fileIsIngestedAndRetrievable() {
        write("warranty.txt", "The ACME-1000 blender carries a 24 month warranty.");

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertTrue(search("How long is the blender warranty?")
                        .stream().anyMatch(text -> text.contains("24 month")),
                        "the file must be ingested and retrievable"));
    }

    @Test
    void longFileIsSplitIntoSegments() {
        // comfortably longer than max-segment-size, so a single document must yield several hits
        write("manual.txt", ("The ACME-2000 mixer needs a 12 volt supply. ").repeat(12));

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertTrue(search("What supply does the mixer need?").stream()
                        .filter(text -> text.contains("ACME-2000")).count() > 1,
                        "a document longer than one segment must be split"));
    }

    /** The escape hatch: any Camel consumer URI feeds a pipeline, the document id in a header. */
    @Test
    void endpointSourceFeedsThePipeline() {
        RestAssured.given().contentType(ContentType.TEXT)
                .body("The custom integration delivers the OMEGA-7 spec sheet.")
                .post("/langchain4j-ingest/feed/custom/integration/omega.txt")
                .then().statusCode(200).body(org.hamcrest.Matchers.is("ingested"));

        assertTrue(search("Which spec sheet is delivered?", "custom")
                .stream().anyMatch(text -> text.contains("OMEGA-7")));
    }

    /** A pipeline declared in Java through {@code @Ingest} rather than in configuration. */
    @Test
    void builderDeclaredPipelineIngests() {
        RestAssured.given().contentType(ContentType.TEXT)
                .body("The builder-declared pipeline handles the SIGMA-3 datasheet.")
                .post("/langchain4j-ingest/feed/built/datasheets/sigma.txt")
                .then().statusCode(200).body(org.hamcrest.Matchers.is("ingested"));

        assertTrue(search("Which datasheet is handled?", "built")
                .stream().anyMatch(text -> text.contains("SIGMA-3")));
    }

    static void write(String name, String content) {
        RestAssured.given().contentType(ContentType.TEXT).body(content)
                .post("/langchain4j-ingest/file/" + name)
                .then().statusCode(204);
    }

    static List<String> search(String query) {
        return search(query, null);
    }

    static List<String> search(String query, String store) {
        var request = RestAssured.given().queryParam("q", query);
        if (store != null) {
            request = request.queryParam("store", store);
        }
        return request
                .get("/langchain4j-ingest/search")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("", String.class);
    }
}
