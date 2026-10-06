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

import java.nio.file.Files;
import java.nio.file.Path;
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
 * The filter configuration, forwarded through the sink Kamelet to the component: id patterns,
 * the size floor and the documentFilter predicate each answer {@code filtered}.
 */
@QuarkusTest
class Langchain4jIngestFilterTest {

    @Test
    void filtersGovernIngestion() {
        assertEquals("filtered", feed("notes.txt", "A text file the id patterns must reject."));
        assertEquals("filtered", feed("draft-plan.md", "Excluded although the include patterns match."));
        assertEquals("filtered", feed("stub.md", "too short"));
        assertEquals("filtered", feed("brief.md", "The CONFIDENTIAL relay specification stays out."));

        assertEquals("ingested", feed("camels.md", "Camels are resilient desert animals with two rows of eyelashes."));

        // only the accepted document reached the store
        assertNotNull(Langchain4jIngestTest.hit("What is resilient?", "filtered", "Camels"));
        assertTrue(Langchain4jIngestTest.hits("What is resilient?", "filtered").stream()
                .allMatch(hit -> hit.get("documentId").equals("camels.md")),
                "no filtered delivery may have been written");
    }

    /** A builder ({@code @Ingest}) pipeline is filtered through configuration the same way. */
    @Test
    void configurationFiltersApplyToBuilderPipelines() {
        String excluded = RestAssured.given().contentType(ContentType.TEXT)
                .body("The OMEGA-3 valve datasheet is not for the knowledge base.")
                .post("/langchain4j-ingest/feed/datasheets/secret-omega.txt")
                .then().statusCode(200).extract().asString();
        assertEquals("filtered", excluded);

        String accepted = RestAssured.given().contentType(ContentType.TEXT)
                .body("The OMEGA-3 valve datasheet lists coolant tolerances.")
                .post("/langchain4j-ingest/feed/datasheets/sheet-omega.txt")
                .then().statusCode(200).extract().asString();
        assertEquals("ingested", accepted);
    }

    /**
     * With a parser, the id patterns are checked before the size guard and the parse: on the
     * {@code htmlfeed} pipeline an excluded document over the cap is answered filtered, while an
     * included one still meets the guard.
     */
    @Test
    void excludedDocumentIsFilteredBeforeTheParse() {
        String oversized = "<html><body><p>" + "x".repeat(5000) + "</p></body></html>";
        assertEquals("filtered", feedProperty("htmlfeed", "clips/clip.mp4", oversized));
        assertTrue(feedProperty("htmlfeed", "big.html", oversized).contains("exceeds maxDocumentSize"));
    }

    /**
     * The same on a directory pipeline: on {@code capped} an excluded file over the cap is filtered,
     * so the file register records it as done instead of the guard failing it on every poll.
     */
    @Test
    void excludedFileIsFilteredBeforeTheParse() throws Exception {
        RestAssured.given().contentType(ContentType.BINARY)
                .body(new byte[1000])
                .post("/langchain4j-ingest/binary/capped/clip.mp4")
                .then().statusCode(204);

        // the file source's register key: absolute path, modification time and size. The register
        // adds it when a poll picks the file up and drops it when the exchange fails, so only a key
        // that stays across poll cycles shows the file is done with
        Path file = Path.of("target/ingest-capped/clip.mp4").toAbsolutePath();
        String key = file + ":" + Files.getLastModifiedTime(file).toMillis() + ":" + Files.size(file);
        Awaitility.await().atMost(30, TimeUnit.SECONDS).during(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> Boolean.parseBoolean(RestAssured.given()
                        .queryParam("repo", "langchain4j-ingest-capped-register")
                        .queryParam("key", key)
                        .get("/langchain4j-ingest/register-contains")
                        .then().statusCode(200).extract().asString()));
    }

    /** Answers the outcome, or the failure message. */
    private static String feedProperty(String pipeline, String documentId, String body) {
        return RestAssured.given().contentType(ContentType.TEXT)
                .body(body)
                .post("/langchain4j-ingest/feed-property/" + pipeline + "/" + documentId)
                .then().statusCode(200).extract().asString();
    }

    private static String feed(String documentId, String body) {
        return RestAssured.given().contentType(ContentType.TEXT)
                .body(body)
                .post("/langchain4j-ingest/feed/filtered/" + documentId)
                .then().statusCode(200).extract().asString();
    }
}
