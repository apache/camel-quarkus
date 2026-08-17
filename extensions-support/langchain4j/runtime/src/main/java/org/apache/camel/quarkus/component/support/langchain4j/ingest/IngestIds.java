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

import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;

/**
 * Deterministic, UUID-formatted segment ids: the same (pipeline, documentId, segmentIndex) always
 * yields the same id, across runs and processes. On stores whose write is an upsert this makes
 * re-ingestion overwrite instead of duplicate; it is the primitive the sync protocol builds on.
 * RFC 4122 name-based UUID, version 5 (SHA-1) — via java-uuid-generator, because the JDK only
 * offers version 3 ({@code UUID.nameUUIDFromBytes}).
 */
public final class IngestIds {

    /**
     * The namespace UUID the v5 derivation requires: its 16 bytes are hashed in front of every
     * name, partitioning our ids from every other system's. Generated once (a random v4 UUID)
     * for this module — the value carries no meaning, only its fixedness matters. It MUST NEVER
     * CHANGE: a different namespace re-keys every corpus, so the next sync duplicates instead of
     * replacing ({@code IngestIdsTest} pins the derived values).
     */
    private static final UUID NAMESPACE = UUID.fromString("2595ee11-cedf-4f54-9086-5ed8775cc26a");

    /**
     * Thread-local because JUG's {@code NameBasedGenerator.generate} synchronizes on one shared
     * {@code MessageDigest} — a single static generator would serialize every segment id of
     * every pipeline on one lock. Lazy per-thread construction also keeps a restricted crypto
     * provider (no SHA-1) surfacing as a diagnosable exception at first use instead of an
     * {@code ExceptionInInitializerError}.
     */
    private static final ThreadLocal<NameBasedGenerator> GENERATOR = ThreadLocal
            .withInitial(() -> Generators.nameBasedGenerator(NAMESPACE));

    private IngestIds() {
    }

    /**
     * The hashed name length-prefixes the pipeline, so the pipeline↔documentId boundary is
     * unambiguous: without it, {@code ("a|b", "c")} and {@code ("a", "b|c")} would hash the same
     * name and two pipelines sharing a store could overwrite (and shrink-delete) each other's
     * segments. The index needs no delimiter discipline — it is last and numeric.
     */
    public static String segmentId(String pipeline, String documentId, int segmentIndex) {
        return GENERATOR.get()
                .generate(pipeline.length() + ":" + pipeline + "|" + documentId + "|" + segmentIndex)
                .toString();
    }
}
