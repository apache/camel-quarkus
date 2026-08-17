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
package org.apache.camel.quarkus.component.langchain4j.ragbridge.it;

import java.util.List;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The retrieval-side isolation hook: a {@code RagRetrievalFilterSupplier} bean filters every
 * retrieval through the produced augmentor — tenant metadata written at ingestion time becomes
 * an actual access control.
 *
 * <p>
 * Each test starts from an emptied default store. That store is shared with the other tests in
 * this profile, and the retriever returns its top 3 matches: without the reset, whether an
 * assertion holds would depend on how many documents earlier tests happened to leave behind.
 */
@QuarkusTest
class RagRetrievalFilterTest {

    private static final String QUESTION = "What does the clause say about the quota?";

    @BeforeEach
    void emptyStore() {
        RestAssured.delete("/rag-bridge/seed").then().statusCode(200);
    }

    @Test
    void tenantFilterIsolatesRetrieval() {
        seedBothTenants();

        // unfiltered: both tenants' documents are retrievable
        List<String> unfiltered = augment(QUESTION, null);
        assertTrue(unfiltered.stream().anyMatch(text -> text.contains("ALPHA-ONLY")), "got: " + unfiltered);
        assertTrue(unfiltered.stream().anyMatch(text -> text.contains("BETA-ONLY")), "got: " + unfiltered);

        // filtered to alpha: beta's documents must be invisible
        RestAssured.post("/rag-bridge/tenant-filter/alpha").then().statusCode(200);
        List<String> filtered = augment(QUESTION, null);
        assertTrue(filtered.stream().anyMatch(text -> text.contains("ALPHA-ONLY")), "got: " + filtered);
        assertFalse(filtered.stream().anyMatch(text -> text.contains("BETA-ONLY")),
                "the tenant filter must isolate retrieval, got: " + filtered);
    }

    /**
     * The tenant taken from the {@code Query} the augmentor passes in, rather than from state the
     * supplier reads elsewhere: this is what makes the hook usable when retrieval does not run on
     * the caller's thread.
     */
    @Test
    void tenantIsDerivedFromTheQuery() {
        seedBothTenants();
        RestAssured.post("/rag-bridge/tenant-filter/from-query").then().statusCode(200);

        List<String> filtered = augment(QUESTION, "beta");
        assertTrue(filtered.stream().anyMatch(text -> text.contains("BETA-ONLY")), "got: " + filtered);
        assertFalse(filtered.stream().anyMatch(text -> text.contains("ALPHA-ONLY")),
                "the chat memory id must select the tenant, got: " + filtered);
    }

    /**
     * The auto-produced default augmentor has neither a name nor a configured store — it is backed
     * by the {@code @Default} beans — and the SPI is told exactly that rather than something made
     * up.
     */
    @Test
    void defaultRagHasEmptyStoreAndAugmentor() {
        RestAssured.delete("/rag-bridge/filter/last-call").then().statusCode(200);

        augment(QUESTION, null);

        RestAssured.get("/rag-bridge/filter/last-augmentor")
                .then().statusCode(200).body(is("null"));
        RestAssured.get("/rag-bridge/filter/last-store")
                .then().statusCode(200).body(is("null"));
    }

    static void seedBothTenants() {
        seed("alpha", "The ALPHA-ONLY clause: tenants of type alpha may exceed the quota.");
        seed("beta", "The BETA-ONLY clause: tenants of type beta must not exceed the quota.");
    }

    static void seed(String tenant, String text) {
        RestAssured.given().contentType(ContentType.TEXT).body(text)
                .post("/rag-bridge/seed/" + tenant)
                .then().statusCode(200);
    }

    static List<String> augment(String question, String memoryId) {
        RequestSpecification request = RestAssured.given().contentType(ContentType.TEXT).body(question);
        if (memoryId != null) {
            request = request.queryParam("memoryId", memoryId);
        }
        return request.post("/rag-bridge/augment")
                .then().statusCode(200)
                .extract().jsonPath().getList("", String.class);
    }
}
