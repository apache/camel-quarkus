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

import io.quarkus.test.QuarkusExtensionTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.Component;
import org.apache.camel.component.file.FileComponent;
import org.apache.camel.quarkus.component.langchain4j.ingest.Ingest;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestPipeline;
import org.apache.camel.quarkus.component.langchain4j.ingest.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * A media document is embedded whole and never parsed, so modality media together with a parser
 * fails the start.
 */
class IngestMediaParserConflictTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Pipelines.class, TestEmbeddingBeans.class))
            .assertException(t -> ValidationTestSupport.assertFailure(t,
                    "Ingestion pipeline 'docs' sets modality 'media' together with a parser"));

    @Test
    void startMustFail() {
        Assertions.fail("The application start was expected to fail");
    }

    @ApplicationScoped
    public static class Pipelines {

        // stands in for camel-quarkus-tika, absent here, so the start gets past the parser
        // presence check to the conflict
        @Produces
        @Singleton
        @Named("tika")
        Component tika() {
            return new FileComponent();
        }

        @Ingest("docs")
        IngestPipeline docs() {
            return IngestPipeline.from(Source.file("target/media-parser-docs"))
                    .parser("tika")
                    .modality("media")
                    .embeddingStore("store")
                    .embeddingModel("model");
        }
    }
}
