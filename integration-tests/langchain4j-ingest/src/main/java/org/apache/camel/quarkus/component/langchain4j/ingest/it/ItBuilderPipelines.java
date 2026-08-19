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
package org.apache.camel.quarkus.component.langchain4j.ingest.it;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.quarkus.component.langchain4j.ingest.Ingest;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestPipeline;
import org.apache.camel.quarkus.component.langchain4j.ingest.Source;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.direct;
import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.kafka;

/**
 * Pipelines declared in Java through the Camel Endpoint DSL: no URI strings anywhere, so every
 * connector option is typed and the compiler checks it. Each of these can still be switched off
 * through {@code quarkus.camel.langchain4j.ingest.<name>.enabled}, which is how the tests that need no
 * broker or object store keep them out of the way.
 */
@ApplicationScoped
public class ItBuilderPipelines {

    @ConfigProperty(name = "minio.endpoint", defaultValue = "http://localhost:9000")
    String minioEndpoint;

    @ConfigProperty(name = "minio.user", defaultValue = "minioadmin")
    String minioUser;

    @ConfigProperty(name = "minio.password", defaultValue = "minioadmin")
    String minioPassword;

    @ConfigProperty(name = "camel.component.kafka.brokers", defaultValue = "localhost:9092")
    String kafkaBrokers;

    @Ingest("datasheets")
    IngestPipeline datasheets() {
        return IngestPipeline.from(Source.endpoint(direct("built-source")))
                .embeddingStore("built-store")
                .embeddingModel("test-model")
                .splitter(120, 20);
    }

    /**
     * The lambda form of the Endpoint DSL — nothing imported, the IDE lists every component off
     * {@code dsl.}. The object key identifies the document, and MinIO addresses buckets by path.
     */
    @Ingest("s3docs")
    IngestPipeline s3docs() {
        return IngestPipeline.from(Source.endpoint(dsl -> dsl.aws2S3("ingest-docs")
                .deleteAfterRead(false)
                .delay(1000)
                .region("us-east-1")
                .forcePathStyle(true)
                .overrideEndpoint(true)
                .uriEndpointOverride(minioEndpoint)
                .accessKey(minioUser)
                .secretKey(minioPassword))
                .documentId("CamelAwsS3Key"))
                .embeddingStore("s3-store")
                .embeddingModel("test-model")
                .splitter(120, 20);
    }

    /** The static-import form of the same DSL; the record key identifies the document. */
    @Ingest("events")
    IngestPipeline events() {
        return IngestPipeline.from(Source.endpoint(kafka("ingest-events")
                .brokers(kafkaBrokers)
                .groupId("ingest")
                .autoOffsetReset("earliest"))
                .documentId("CamelKafkaKey"))
                .embeddingStore("events-store")
                .embeddingModel("test-model")
                .splitter(120, 20);
    }
}
