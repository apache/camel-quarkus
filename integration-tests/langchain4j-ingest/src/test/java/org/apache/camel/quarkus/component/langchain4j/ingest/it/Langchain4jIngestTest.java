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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Configuration alone turns a directory into a knowledge base: a file dropped into the watched
 * directory is split, embedded and stored, with no route and no ingestion code written by the
 * application.
 */
@QuarkusTest
class Langchain4jIngestTest {

    @Test
    void fileIsIngestedAndStored() {
        write("warranty.txt", "The ACME-1000 blender carries a 24 month warranty.");

        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Map<String, String> hit = hit("How long is the blender warranty?", null, "24 month");
                    assertNotNull(hit, "the file must be ingested and its segments stored");
                    // the metadata stamped on every segment is what retrieval cites documents by
                    assertEquals("products", hit.get("pipeline"));
                    assertEquals("warranty.txt", hit.get("documentId"));
                });
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

    /**
     * The escape hatch: any Camel consumer URI feeds a pipeline. The document id arrives in the
     * default header and is read through the {@code $simple{...}} expression the {@code custom}
     * pipeline configures — the properties-safe form of the simple language.
     */
    @Test
    void endpointSourceFeedsThePipeline() {
        RestAssured.given().contentType(ContentType.TEXT)
                .body("The custom integration delivers the OMEGA-7 spec sheet.")
                .post("/langchain4j-ingest/feed/custom/integration/omega.txt")
                .then().statusCode(200).body(org.hamcrest.Matchers.is("ingested"));

        Map<String, String> hit = hit("Which spec sheet is delivered?", "custom", "OMEGA-7");
        assertNotNull(hit, "the fed document must be ingested");
        assertEquals("custom", hit.get("pipeline"));
        assertEquals("integration/omega.txt", hit.get("documentId"));
    }

    /** A pipeline declared in Java through {@code @Ingest} rather than in configuration. */
    @Test
    void builderDeclaredPipelineIngests() {
        RestAssured.given().contentType(ContentType.TEXT)
                .body("The builder-declared pipeline handles the SIGMA-3 datasheet.")
                .post("/langchain4j-ingest/feed/built/datasheets/sigma.txt")
                .then().statusCode(200).body(org.hamcrest.Matchers.is("ingested"));

        Map<String, String> hit = hit("Which datasheet is handled?", "built", "SIGMA-3");
        assertNotNull(hit, "the fed document must be ingested");
        // the metadata names the pipeline (@Ingest("datasheets")), not the store it writes to
        assertEquals("datasheets", hit.get("pipeline"));
        assertEquals("datasheets/sigma.txt", hit.get("documentId"));
    }

    static void write(String name, String content) {
        RestAssured.given().contentType(ContentType.TEXT).body(content)
                .post("/langchain4j-ingest/file/" + name)
                .then().statusCode(204);
    }

    static List<String> search(String query) {
        return hits(query, null).stream().map(hit -> hit.get("text")).toList();
    }

    /** The first stored segment matching the query whose text contains the marker, or null. */
    static Map<String, String> hit(String query, String store, String marker) {
        return hits(query, store).stream()
                .filter(hit -> hit.get("text").contains(marker))
                .findFirst().orElse(null);
    }

    /** Every stored segment matching the query, as (text, pipeline, documentId) maps. */
    static List<Map<String, String>> hits(String query, String store) {
        var request = RestAssured.given().queryParam("q", query);
        if (store != null) {
            request = request.queryParam("store", store);
        }
        return request
                .get("/langchain4j-ingest/search")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("");
    }
}
