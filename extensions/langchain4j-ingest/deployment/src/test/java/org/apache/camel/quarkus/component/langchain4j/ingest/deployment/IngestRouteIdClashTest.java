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
import org.apache.camel.quarkus.component.langchain4j.ingest.Ingest;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestPipeline;
import org.apache.camel.quarkus.component.langchain4j.ingest.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * A pipeline named after a step of another one would share a route id with it: the start fails
 * naming both, whichever way each is declared.
 */
class IngestRouteIdClashTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestEmbeddingBeans.class, Pipelines.class))
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.docs-sink.source.directory", "target/route-id-clash-sink")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.docs-sink.embedding-store", "store")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.docs-sink.embedding-model", "model")
            .assertException(t -> ValidationTestSupport.assertFailure(t,
                    "Ingestion pipelines 'docs' and 'docs-sink' would both use the route id 'langchain4j-ingest-docs-sink'"));

    @Test
    void startMustFail() {
        Assertions.fail("The application start was expected to fail");
    }

    @ApplicationScoped
    public static class Pipelines {

        @Ingest("docs")
        IngestPipeline docs() {
            return IngestPipeline.from(Source.file("target/route-id-clash"))
                    .embeddingStore("store")
                    .embeddingModel("model");
        }
    }
}
