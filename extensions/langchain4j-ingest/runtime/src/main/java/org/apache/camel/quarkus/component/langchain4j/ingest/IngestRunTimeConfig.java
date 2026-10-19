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
package org.apache.camel.quarkus.component.langchain4j.ingest;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

/**
 * Runtime configuration of ingestion pipelines: concrete locations and switches that may differ
 * per deployment. The pipeline topology is build-time, see {@link IngestBuildTimeConfig}.
 */
@ConfigMapping(prefix = "quarkus.camel.langchain4j.ingest")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface IngestRunTimeConfig {

    /**
     * Ingestion pipelines by name.
     */
    @WithParentName
    @ConfigDocMapKey("pipeline-name")
    Map<String, PipelineRunTimeConfig> pipelines();

    interface PipelineRunTimeConfig {

        /**
         * Whether this pipeline starts. Useful to switch ingestion off in dev mode.
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * The document source.
         */
        SourceRunTimeConfig source();

        /**
         * Filters deciding which deliveries are ingested. A rejected delivery is answered with
         * a `filtered` outcome and never keeps the sink's dedup claim; a directory pipeline's file
         * register still records the file, so it is not read again until it changes.
         */
        FilterRunTimeConfig filter();

        interface FilterRunTimeConfig {

            /**
             * Comma-separated Ant-style patterns the document id must match to be ingested,
             * for example `++**++/++*++.pdf,++**++/++*++.md` (`++*++.pdf` alone misses files in
             * subdirectories). Matching is case-sensitive, and an id starting with `/` needs a
             * pattern starting with `/`. A non-matching delivery is rejected before the sink's dedup
             * claim, without reading the body and, with a `parser`, before the parse. When not set,
             * every id is accepted.
             */
            Optional<String> includeId();

            /**
             * Comma-separated Ant-style patterns for document ids to skip, for example
             * `++**++/draft-++*++`. Exclusion wins over `include-id`.
             */
            Optional<String> excludeId();

            /**
             * Minimum size of one document in characters, in bytes with `modality=media`; 0, the
             * default, means no minimum. A shorter document is answered `filtered` instead of being
             * written.
             */
            @WithDefault("0")
            int minDocumentSize();

            /**
             * Name of a Camel `Predicate` bean deciding whether a delivery is ingested,
             * evaluated with the body available. Looked up by name only.
             */
            Optional<String> documentFilter();
        }

        interface SourceRunTimeConfig {

            /**
             * The directory to ingest documents from, for a pipeline that has no `source.uri`. A
             * path is a deployment concern, so unlike the URI it stays runtime configuration.
             * Setting both is an error.
             */
            Optional<String> directory();

            /**
             * Whether subdirectories are ingested too, when reading a directory.
             */
            @WithDefault("true")
            boolean recursive();

            /**
             * Name of the `IdempotentRepository` bean remembering already ingested documents,
             * instead of the built-in in-memory one (100 000 keys, lost on restart). Looked up
             * by name only. On a pipeline consuming from a component it deduplicates deliveries
             * by document id, first write wins.
             */
            Optional<String> idempotentRepository();

            /**
             * When `true`, an in-memory register (100 000 keys) is created and bound under the
             * `idempotent-repository` name, unless a bean with that name exists — the existing
             * bean wins.
             */
            @WithDefault("false")
            boolean idempotentRepositoryAutoCreate();

            /**
             * Where the document id lives in the exchange the consumer delivers: normally the
             * name of a header, such as `CamelAwsS3Key` for an S3 consumer or `CamelKafkaKey` for
             * a Kafka one. For an id that is not a plain header, write a simple-language
             * expression in the `+$simple{...}+` form — MicroProfile Config passes it through
             * untouched, while a `+${...}+` in a properties file would be consumed as a config
             * expansion before Camel ever saw it. When not set, a pipeline reading a directory
             * uses the file name, and one consuming from a component uses the
             * `CamelLangChain4jIngestDocumentId` header (the deprecated 3.39 name
             * `CamelIngestDocumentId` is still read as a fallback).
             */
            Optional<String> documentId();
        }
    }
}
