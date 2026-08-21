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
             * `CamelIngestDocumentId` header.
             */
            Optional<String> documentId();
        }
    }
}
