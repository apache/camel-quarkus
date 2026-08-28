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
 * The race this extension must win: a {@code camel.component.<scheme>.*} property referencing a
 * missing component makes Camel Main's property auto-configuration fail with its bare classpath
 * message before any route builder runs. The pre-start task gets there first, so the user reads
 * the artifact hint, not the race's loser.
 */
class IngestMissingComponentWithComponentPropertyTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Pipelines.class, TestEmbeddingBeans.class))
            .overrideConfigKey("camel.component.direct.block", "false")
            .assertException(t -> ValidationTestSupport.assertFailure(t,
                    "component 'direct' is not on the classpath",
                    "org.apache.camel.quarkus:camel-quarkus-direct"));

    @Test
    void startMustFail() {
        Assertions.fail("The application start was expected to fail");
    }

    @ApplicationScoped
    public static class Pipelines {

        @Ingest("docs")
        IngestPipeline docs() {
            return IngestPipeline.from(Source.endpoint("direct:feed"))
                    .embeddingStore("store")
                    .embeddingModel("model");
        }
    }
}
