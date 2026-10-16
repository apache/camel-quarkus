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
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The pipelines load their Kamelets from the classpath whatever {@code camel.component.kamelet.location}
 * says, so an application keeping its own Kamelets elsewhere does not break them.
 */
class IngestKameletLocationTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestEmbeddingBeans.class))
            .overrideConfigKey("camel.component.kamelet.location", "classpath:my-kamelets")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.docs.source.directory", "target/kamelet-location-docs")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.docs.embedding-store", "store")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.docs.embedding-model", "model");

    @Inject
    CamelContext context;

    @Test
    void pipelineStartsWithAnotherKameletLocation() {
        Assertions.assertNotNull(context.getRoute("langchain4j-ingest-docs-source"));
        Assertions.assertNotNull(context.getRoute("langchain4j-ingest-docs-sink"));
    }
}
