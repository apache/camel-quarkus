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

import java.util.Locale;

/**
 * Outcome of one ingestion operation.
 */
public record IngestResult(String pipeline, String documentId, int segmentsWritten, Outcome outcome) {

    public enum Outcome {
        /** New document written. */
        INGESTED,
        /** Previous vectors overwritten/removed. */
        REPLACED,
        /** Change detection short-circuited — no read, no embedding, no store write. */
        SKIPPED_UNCHANGED,
        /** Blank document, nothing written. */
        EMPTY,
        /** Vectors removed. */
        DELETED,
        /** The document was explicitly deleted and stays deleted. */
        SUPPRESSED_TOMBSTONE,
        /** An API correction wins over the source until unpinned. */
        SUPPRESSED_PINNED,
        /** A poison document skipped because a previous attempt failed and its content is unchanged. */
        DEAD_LETTERED;

        /** The stable wire/log form, e.g. {@code skipped-unchanged}. */
        public String label() {
            return name().toLowerCase(Locale.ROOT).replace('_', '-');
        }
    }
}
