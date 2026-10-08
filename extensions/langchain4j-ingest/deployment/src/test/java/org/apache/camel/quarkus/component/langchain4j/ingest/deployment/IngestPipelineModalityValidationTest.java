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
package org.apache.camel.quarkus.component.langchain4j.ingest.deployment;

import org.apache.camel.quarkus.component.langchain4j.ingest.IngestPipeline;
import org.apache.camel.quarkus.component.langchain4j.ingest.Source;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The builder rejects a bad modality or a blank content type eagerly, like the configuration path. */
class IngestPipelineModalityValidationTest {

    @Test
    void unknownOrMissingModalityRejected() {
        for (String modality : new String[] { "sound", null }) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> IngestPipeline.from(Source.file("target/x")).modality(modality));
            assertTrue(e.getMessage().contains("modality must be 'text' or 'media'"), e.getMessage());
        }
    }

    @Test
    void blankContentTypeRejected() {
        // parameters alone would be dropped by the component, leaving the content type unset
        for (String contentType : new String[] { " ", null, "; codecs=opus" }) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> IngestPipeline.from(Source.file("target/x")).contentType(contentType));
            assertTrue(e.getMessage().contains("content-type must not be blank or only parameters"), e.getMessage());
        }
    }

    @Test
    void validValuesAccepted() {
        assertDoesNotThrow(() -> IngestPipeline.from(Source.file("target/x"))
                .modality("text")
                .modality("Media")
                .contentType("audio/wav"));
    }
}
