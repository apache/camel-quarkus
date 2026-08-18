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

import java.util.Optional;

/**
 * A pipeline declared in Java rather than in configuration, returned from an {@link Ingest}
 * method. Every property has a configuration twin, and both paths share the same runtime.
 */
public final class IngestPipeline {

    private final Source source;
    private String embeddingStoreName;
    private String embeddingModelName;
    private int maxSegmentSize = IngestBuildTimeConfig.DEFAULT_MAX_SEGMENT_SIZE;
    private int maxOverlapSize = IngestBuildTimeConfig.DEFAULT_MAX_OVERLAP_SIZE;

    private IngestPipeline(Source source) {
        this.source = source;
    }

    public static IngestPipeline from(Source source) {
        return new IngestPipeline(source);
    }

    public IngestPipeline embeddingStore(String beanName) {
        this.embeddingStoreName = beanName;
        return this;
    }

    public IngestPipeline embeddingModel(String beanName) {
        this.embeddingModelName = beanName;
        return this;
    }

    public IngestPipeline splitter(int maxSegmentSize, int maxOverlapSize) {
        // the same rule the configuration path is held to at build time, so both paths reject
        // the same values rather than failing later inside the splitter
        if (maxSegmentSize <= 0 || maxOverlapSize < 0 || maxOverlapSize >= maxSegmentSize) {
            throw new IllegalArgumentException("max-segment-size must be positive and max-overlap-size must be "
                    + "smaller than it (got " + maxSegmentSize + " / " + maxOverlapSize + ")");
        }
        this.maxSegmentSize = maxSegmentSize;
        this.maxOverlapSize = maxOverlapSize;
        return this;
    }

    String sourceType() {
        return source.type();
    }

    String sourceUri() {
        return source.uri();
    }

    Optional<String> embeddingStoreName() {
        return Optional.ofNullable(embeddingStoreName);
    }

    Optional<String> embeddingModelName() {
        return Optional.ofNullable(embeddingModelName);
    }

    int maxSegmentSize() {
        return maxSegmentSize;
    }

    int maxOverlapSize() {
        return maxOverlapSize;
    }

    /** The configuration view, so a builder pipeline reuses every configuration path verbatim. */
    IngestRunTimeConfig.PipelineRunTimeConfig asRunTimeConfig() {
        IngestRunTimeConfig.PipelineRunTimeConfig.SourceRunTimeConfig sourceConfig = source.asRunTimeConfig();
        return new IngestRunTimeConfig.PipelineRunTimeConfig() {

            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public SourceRunTimeConfig source() {
                return sourceConfig;
            }
        };
    }
}
