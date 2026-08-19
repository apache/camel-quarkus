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

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A pipeline declared in Java rather than in configuration, returned from an {@link Ingest}
 * method. Every property has a configuration twin, and both paths share the same runtime.
 */
public final class IngestPipeline {

    /** The values {@link #parser(String)} and the {@code parser} configuration property accept. */
    public static final Set<String> SUPPORTED_PARSERS = Arrays.stream(IngestParsers.Parser.values())
            .map(IngestParsers.Parser::configName)
            .collect(Collectors.toUnmodifiableSet());

    private final Source source;
    private String embeddingStoreName;
    private String embeddingModelName;
    private String parser;
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

    /**
     * Parses the consumed payload into text before splitting: {@code tika} extracts plain text
     * in-process, {@code docling} converts to markdown through a Docling Serve instance. The
     * corresponding extension must be on the classpath.
     */
    public IngestPipeline parser(String parser) {
        // the same rule the configuration path is held to at build time; the null check comes
        // first because the unmodifiable set's contains(null) throws a bare NPE
        if (parser == null || !SUPPORTED_PARSERS.contains(parser)) {
            throw new IllegalArgumentException("parser must be one of " + SUPPORTED_PARSERS
                    + " (got '" + parser + "')");
        }
        this.parser = parser;
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

    Optional<String> parser() {
        return Optional.ofNullable(parser);
    }

    int maxSegmentSize() {
        return maxSegmentSize;
    }

    int maxOverlapSize() {
        return maxOverlapSize;
    }

    /** The build-time configuration view — the twin of {@link #asRunTimeConfig()}. */
    IngestBuildTimeConfig.PipelineBuildTimeConfig asBuildTimeConfig() {
        return new IngestBuildTimeConfig.PipelineBuildTimeConfig() {

            @Override
            public Optional<String> parser() {
                return Optional.ofNullable(parser);
            }

            @Override
            public Optional<String> embeddingStore() {
                return Optional.ofNullable(embeddingStoreName);
            }

            @Override
            public Optional<String> embeddingModel() {
                return Optional.ofNullable(embeddingModelName);
            }

            @Override
            public int maxSegmentSize() {
                return maxSegmentSize;
            }

            @Override
            public int maxOverlapSize() {
                return maxOverlapSize;
            }

            @Override
            public SourceBuildTimeConfig source() {
                // a file-declared source has no URI, which is exactly how the shared creation
                // path tells the two source kinds apart
                return () -> Optional.ofNullable(source.uri());
            }
        };
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
