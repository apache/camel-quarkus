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
import java.util.function.Function;

import org.apache.camel.builder.EndpointConsumerBuilder;
import org.apache.camel.builder.endpoint.EndpointBuilderFactory;

/**
 * Where a builder-declared pipeline reads from: the typed twin of
 * {@code quarkus.camel.langchain4j.ingest.<name>.source}.
 *
 * <p>
 * Reach for {@link #endpoint(Function)} and let the IDE do the finding — type {@code dsl.} and
 * it lists a factory for every Camel component, {@code aws2S3}, {@code kafka}, {@code ftp} and
 * some 300 more, each completing its own options:
 *
 * <pre>
 * Source.endpoint(dsl -&gt; dsl.aws2S3("product-docs").region("eu-west-1").deleteAfterRead(false)).documentId("CamelAwsS3Key")
 * </pre>
 *
 * {@link #endpoint(EndpointConsumerBuilder)} is the same DSL through a static import,
 * {@link #file(String)} the shorthand for a directory, and {@link #endpoint(String)} a raw URI for
 * the cases where one is assembled at runtime.
 */
public final class Source {

    /** All ~300 component factories in one object; the interface has no abstract methods. */
    private static final EndpointBuilderFactory DSL = new EndpointBuilderFactory() {
    };

    private final String type;
    private String directory;
    private String uri;
    private String documentId;
    private boolean recursive = true;

    private Source(String type) {
        this.type = type;
    }

    public static Source file(String directory) {
        Source source = new Source("file");
        source.directory = requireText(directory, "directory");
        return source;
    }

    /** A raw consumer URI. Prefer {@link #endpoint(org.apache.camel.builder.EndpointConsumerBuilder)}. */
    public static Source endpoint(String uri) {
        Source source = new Source("endpoint");
        source.uri = requireText(uri, "uri");
        return source;
    }

    /**
     * The Camel Endpoint DSL, reached through a lambda: type {@code dsl.} and the IDE lists a
     * factory for every Camel component, each completing its own options.
     *
     * <pre>
     * Source.endpoint(dsl -&gt; dsl.aws2S3("product-docs").region("eu-west-1").deleteAfterRead(false))
     * </pre>
     *
     * Nothing to import and nothing to look up, which is why this is the form to reach for. Note
     * the sharp edge it shares with the overload below: the DSL factories all ship in one
     * artifact, so the call compiles even when the connector extension is absent, and the missing
     * component is reported at startup rather than at compile time — with the same error naming
     * the missing extension that the build gives for configured URIs.
     */
    public static Source endpoint(Function<EndpointBuilderFactory, EndpointConsumerBuilder> source) {
        return endpoint(source.apply(DSL));
    }

    /**
     * The same DSL reached through a static import, for those who prefer it:
     *
     * <pre>
     * import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.aws2S3;
     *
     * Source.endpoint(aws2S3("product-docs").region("eu-west-1").deleteAfterRead(false))
     * </pre>
     */
    public static Source endpoint(EndpointConsumerBuilder builder) {
        return endpoint(builder.getRawUri());
    }

    public Source recursive(boolean recursive) {
        this.recursive = recursive;
        return this;
    }

    public Source documentId(String expression) {
        this.documentId = requireText(expression, "documentId");
        return this;
    }

    /** A null or blank value here would surface much later as an obscure Camel error. */
    private static String requireText(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Source." + what + " must not be null or blank");
        }
        return value;
    }

    String type() {
        return type;
    }

    String uri() {
        return uri;
    }

    /** The configuration view, so builder pipelines reuse every configuration path verbatim. */
    IngestRunTimeConfig.PipelineRunTimeConfig.SourceRunTimeConfig asRunTimeConfig() {
        return new IngestRunTimeConfig.PipelineRunTimeConfig.SourceRunTimeConfig() {

            @Override
            public Optional<String> directory() {
                return Optional.ofNullable(directory);
            }

            @Override
            public boolean recursive() {
                return recursive;
            }

            @Override
            public Optional<String> documentId() {
                return Optional.ofNullable(documentId);
            }
        };
    }
}
