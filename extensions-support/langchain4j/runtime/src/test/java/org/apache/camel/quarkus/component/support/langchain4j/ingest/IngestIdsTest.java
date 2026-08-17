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
package org.apache.camel.quarkus.component.support.langchain4j.ingest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Determinism across releases is the persistent contract: these ids live in users' vector stores
 * forever, and any change to the derivation (namespace UUID, separator, hash) silently re-keys
 * every corpus — the next sync then duplicates instead of replacing. The golden values below must
 * therefore never change; a failure here is a released-data compatibility break, not a test to
 * update.
 */
class IngestIdsTest {

    @Test
    void goldenSegmentIdsNeverChange() {
        // the expected UUIDs are pre-generated: they were produced by running segmentId() itself
        // at the moment the derivation (namespace + length-prefixed name + UUIDv5) was frozen,
        // and copied here verbatim — the assertions pin that historical output, they are not
        // independently computed values
        assertEquals("2de4b222-3db1-599f-9e7e-b1825adfe766", IngestIds.segmentId("p", "doc", 0));
        assertEquals("4cea268f-02a9-5d9f-bfa9-940630ef23f0", IngestIds.segmentId("products", "manuals/spec.pdf", 3));
    }

    @Test
    void separatorCannotShiftThePipelineDocumentBoundary() {
        assertNotEquals(IngestIds.segmentId("a|b", "c", 0), IngestIds.segmentId("a", "b|c", 0),
                "length-prefixing the pipeline must keep two pipelines sharing a store from colliding");
    }

    @Test
    void idsAreDeterministicAndDistinct() {
        assertEquals(IngestIds.segmentId("p", "doc", 1), IngestIds.segmentId("p", "doc", 1));
        assertNotEquals(IngestIds.segmentId("p", "doc", 0), IngestIds.segmentId("p", "doc", 1));
        assertNotEquals(IngestIds.segmentId("p", "doc", 0), IngestIds.segmentId("q", "doc", 0));
        assertNotEquals(IngestIds.segmentId("p", "doc", 0), IngestIds.segmentId("p", "cod", 0));
    }
}
